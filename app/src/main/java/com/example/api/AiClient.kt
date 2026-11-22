package com.example.api

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
    NO_KEY("AI is not available. Log in to your account, and make sure the server has NVIDIA_API_KEY set."),
    LOGIN("Please log in again to use AI."),
    NO_INTERNET("No internet connection. Check it and tap Retry."),
    TIMEOUT("The AI took too long to answer. Tap Retry."),
    BAD_KEY("The server's NVIDIA API key was not accepted. The server owner must check NVIDIA_API_KEY."),
    RATE_LIMIT("Too many requests right now. Wait a few seconds and tap Retry."),
    QUOTA("You have used all your AI requests for today. They reset tomorrow."),
    SERVER("The AI service is busy. Tap Retry in a moment."),
    MODEL_GONE("NVIDIA has retired every AI model the server tried. Please try again later."),
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
 * AI requests through the AutoResume backend (POST /api/ai/chat), which holds the NVIDIA key and picks a live
 * NVIDIA model. Nothing secret is built into the app any more, and a retired model never needs an app update.
 *
 * It needs the backend address (BACKEND_URL) and a logged-in account: [tokenProvider] returns the login token.
 * Every failure comes back as [AiResult.Fail] with a reason; nothing is ever invented.
 */
object AiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Set once by the app (the ViewModel): returns the current login token, or "" when logged out. */
    @Volatile var tokenProvider: () -> String = { "" }

    val isConfigured: Boolean get() = BackendClient.isConfigured && tokenProvider().isNotBlank()

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
            .put("messages", arr)
            .put("maxTokens", maxTokens)
            .put("temperature", temperature)
            .put("stream", stream)
            .toString()
    }

    private fun request(json: String, stream: Boolean): Request = Request.Builder()
        .url(BackendClient.urlOf("/api/ai/chat"))
        .header("Authorization", "Bearer ${tokenProvider()}")
        .header("Accept", if (stream) "text/event-stream" else "application/json")
        .post(json.toRequestBody("application/json".toMediaType()))
        .build()

    /** The backend's error codes are named like [AiError]; only a few need translating. */
    private fun failFrom(status: Int, code: String, detail: String?): AiResult.Fail {
        val error = when {
            status == 401 -> AiError.LOGIN
            code == "NO_KEY" -> AiError.NO_KEY
            code == "BAD_KEY" -> AiError.BAD_KEY
            code == "RATE_LIMIT" || code == "BUSY" -> AiError.RATE_LIMIT
            code == "QUOTA" -> AiError.QUOTA
            code == "TIMEOUT" -> AiError.TIMEOUT
            code == "NO_INTERNET" || code == "SERVER" -> AiError.SERVER // the server could not reach NVIDIA, not the phone
            code == "MODEL_GONE" -> AiError.MODEL_GONE
            code == "EMPTY" -> AiError.EMPTY
            else -> AiError.OTHER
        }
        val extra = detail ?: if (status == 404) "The backend has no AI routes yet. Update the backend." else null
        return AiResult.Fail(error, extra)
    }

    private fun failFromBody(status: Int, raw: String): AiResult.Fail {
        val obj = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
        val detail = if (obj.has("detail") && !obj.isNull("detail")) obj.optString("detail", "").ifBlank { null } else null
        return failFrom(status, if (obj.has("error")) obj.optString("error", "") else "", detail)
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
                        if (!resp.isSuccessful) {
                            failFromBody(resp.code, raw)
                        } else {
                            val obj = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
                            val cleaned = clean(if (obj.has("text") && !obj.isNull("text")) obj.optString("text", "") else "")
                            if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY) else AiResult.Ok(cleaned)
                        }
                    }
                } finally {
                    handle?.dispose()
                }
            } catch (e: Exception) {
                exceptionError(e)
            }
        }

    /**
     * Streams the answer (server-sent events from the backend). [onText] receives the full cleaned text so far,
     * every time it grows. It is called on a background thread, so only write it into a StateFlow or similar.
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
                    if (!resp.isSuccessful) {
                        failFromBody(resp.code, resp.body?.string().orEmpty())
                    } else {
                        val source = resp.body?.source()
                        var event = ""
                        var finalText: String? = null
                        var failure: AiResult.Fail? = null
                        if (source != null) {
                            while (true) {
                                val line = source.readUtf8Line() ?: break
                                if (line.startsWith("event:")) { event = line.removePrefix("event:").trim(); continue }
                                if (!line.startsWith("data:")) continue
                                val obj = try { JSONObject(line.removePrefix("data:").trim()) } catch (e: Exception) { continue }
                                val text = if (obj.has("text") && !obj.isNull("text")) obj.optString("text", "") else ""
                                when (event) {
                                    "text" -> if (text.isNotEmpty()) onText(clean(text))
                                    "done" -> { finalText = text; break }
                                    "error" -> {
                                        val detail = if (obj.has("detail") && !obj.isNull("detail")) obj.optString("detail", "").ifBlank { null } else null
                                        failure = failFrom(502, if (obj.has("error")) obj.optString("error", "") else "", detail)
                                        break
                                    }
                                }
                            }
                        }
                        failure ?: run {
                            val cleaned = clean(finalText.orEmpty())
                            if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY) else AiResult.Ok(cleaned)
                        }
                    }
                }
            } finally {
                handle?.dispose()
            }
        } catch (e: Exception) {
            exceptionError(e)
        }
    }
}
