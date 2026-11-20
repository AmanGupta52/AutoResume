package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class AiError(val message: String) {
    NO_KEY("AI is not set up yet. Add your NVIDIA key in Settings."),
    NO_INTERNET("No internet connection. Check it and tap Retry."),
    TIMEOUT("The AI took too long to answer. Tap Retry."),
    BAD_KEY("NVIDIA did not accept the key. Check it in Settings."),
    RATE_LIMIT("Too many requests right now. Wait a few seconds and tap Retry."),
    SERVER("The AI service is busy. Tap Retry in a moment."),
    EMPTY("The AI sent an empty answer. Tap Retry."),
    OTHER("Something went wrong. Tap Retry."),
}

sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Fail(val error: AiError, val detail: String? = null) : AiResult()
}

/** One message of a conversation. role = "system", "user" or "assistant". */
data class AiMessage(val role: String, val content: String)

/**
 * AI requests through NVIDIA's hosted, OpenAI-compatible API (https://build.nvidia.com).
 * The key comes from `.env` at build time, or from [keyOverride] when the person types one in Settings.
 * Nothing here ever invents text: every failure comes back as [AiResult.Fail] with a reason.
 */
object AiClient {
    private const val URL = "https://integrate.api.nvidia.com/v1/chat/completions"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Key typed by the person in Settings. Wins over the build-time key. */
    @Volatile var keyOverride: String = ""

    private val key: String
        get() = keyOverride.trim().ifBlank { BuildConfig.NVIDIA_API_KEY.trim() }
    private val model: String
        get() = BuildConfig.NVIDIA_MODEL.trim().ifBlank { "meta/llama-3.3-70b-instruct" }

    val isConfigured: Boolean get() = key.isNotBlank() && key != "MY_NVIDIA_API_KEY"

    /** Removes <think>...</think> (also an unfinished one while streaming) and stray markdown fences. */
    fun clean(s: String): String = s
        .replace(Regex("<think>[\\s\\S]*?</think>"), "")
        .replace(Regex("<think>[\\s\\S]*$"), "")
        .replace("```", "")
        .trim()

    private fun body(messages: List<AiMessage>, maxTokens: Int, temperature: Double, stream: Boolean): String {
        val arr = JSONArray()
        messages.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
        return JSONObject()
            .put("model", model)
            .put("max_tokens", maxTokens)
            .put("temperature", temperature)
            .put("stream", stream)
            .put("messages", arr)
            .toString()
    }

    private fun request(json: String, stream: Boolean): Request = Request.Builder()
        .url(URL)
        .header("Authorization", "Bearer $key")
        .header("Accept", if (stream) "text/event-stream" else "application/json")
        .post(json.toRequestBody("application/json".toMediaType()))
        .build()

    private fun httpError(code: Int, text: String): AiResult.Fail = when {
        code == 401 || code == 403 -> AiResult.Fail(AiError.BAD_KEY)
        code == 429 -> AiResult.Fail(AiError.RATE_LIMIT)
        code in 500..599 -> AiResult.Fail(AiError.SERVER)
        else -> AiResult.Fail(AiError.OTHER, "HTTP $code ${text.take(160)}")
    }

    private fun exceptionError(e: Exception): AiResult.Fail = when (e) {
        is UnknownHostException, is ConnectException -> AiResult.Fail(AiError.NO_INTERNET)
        is SocketTimeoutException -> AiResult.Fail(AiError.TIMEOUT)
        else -> AiResult.Fail(AiError.OTHER, e.message)
    }

    /** Whole answer at once. */
    suspend fun ask(system: String, user: String, maxTokens: Int = 700, temperature: Double = 0.4): AiResult =
        askMessages(listOf(AiMessage("system", system), AiMessage("user", user)), maxTokens, temperature)

    suspend fun askMessages(messages: List<AiMessage>, maxTokens: Int = 700, temperature: Double = 0.4): AiResult =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext AiResult.Fail(AiError.NO_KEY)
            try {
                val call = http.newCall(request(body(messages, maxTokens, temperature, false), false))
                val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { if (it != null) call.cancel() }
                try {
                    call.execute().use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) return@withContext httpError(resp.code, raw)
                        val text = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("message")?.optString("content").orEmpty()
                        val cleaned = clean(text)
                        if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY) else AiResult.Ok(cleaned)
                    }
                } finally {
                    handle?.dispose()
                }
            } catch (e: Exception) {
                exceptionError(e)
            }
        }

    /**
     * Streams the answer. [onText] receives the full cleaned text so far, every time it grows.
     * It is called on a background thread, so only write it into a StateFlow or similar.
     */
    suspend fun stream(
        messages: List<AiMessage>,
        maxTokens: Int = 700,
        temperature: Double = 0.4,
        onText: (String) -> Unit,
    ): AiResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext AiResult.Fail(AiError.NO_KEY)
        try {
            val call = http.newCall(request(body(messages, maxTokens, temperature, true), true))
            val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { if (it != null) call.cancel() }
            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext httpError(resp.code, resp.body?.string().orEmpty())
                    val source = resp.body?.source() ?: return@withContext AiResult.Fail(AiError.EMPTY)
                    val raw = StringBuilder()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        val delta = try {
                            JSONObject(data).optJSONArray("choices")?.optJSONObject(0)
                                ?.optJSONObject("delta")?.optString("content").orEmpty()
                        } catch (e: Exception) { "" }
                        if (delta.isNotEmpty()) {
                            raw.append(delta)
                            onText(clean(raw.toString()))
                        }
                    }
                    val cleaned = clean(raw.toString())
                    if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY) else AiResult.Ok(cleaned)
                }
            } finally {
                handle?.dispose()
            }
        } catch (e: Exception) {
            exceptionError(e)
        }
    }
}
