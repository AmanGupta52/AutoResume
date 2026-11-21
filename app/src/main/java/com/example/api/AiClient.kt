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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class AiError(val message: String) {
    NO_KEY("AI is not set up in this build. Add NVIDIA_API_KEY to your GitHub secrets and rebuild the app."),
    NO_INTERNET("No internet connection. Check it and tap Retry."),
    TIMEOUT("The AI took too long to answer. Tap Retry."),
    BAD_KEY("NVIDIA did not accept the API key in this build. Check the NVIDIA_API_KEY secret and rebuild."),
    RATE_LIMIT("Too many requests right now. Wait a few seconds and tap Retry."),
    SERVER("The AI service is busy. Tap Retry in a moment."),
    MODEL_GONE("NVIDIA has retired every AI model this app tried. Rebuild the app to refresh the model list."),
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
 *
 * The key is baked in at build time from the GitHub secret NVIDIA_API_KEY (see .env.example).
 *
 * NVIDIA retires free models every few weeks and answers HTTP 410 (or 404) for them, so a fixed model
 * name always breaks sooner or later. This client therefore:
 *  1. tries NVIDIA_MODEL first when one is set,
 *  2. asks NVIDIA which chat models are live right now (GET /v1/models) and ranks them,
 *  3. skips any model that answers 410/404 and moves on to the next one,
 *  4. remembers the model that worked for the rest of the session.
 * Every failure comes back as [AiResult.Fail] with a reason; nothing is ever invented.
 */
object AiClient {
    private const val BASE = "https://integrate.api.nvidia.com/v1"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val key: String get() = BuildConfig.NVIDIA_API_KEY.trim().trim('"', '\'', ' ')

    /** Optional pin from NVIDIA_MODEL. Blank or "auto" means: pick a live model automatically. */
    private val configuredModel: String
        get() = BuildConfig.NVIDIA_MODEL.trim().trim('"', '\'', ' ').let { if (it.equals("auto", ignoreCase = true)) "" else it }

    val isConfigured: Boolean get() = key.isNotBlank() && key != "MY_NVIDIA_API_KEY"

    // ---------------------------------------------------------------- model selection

    private val dead = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var working: String? = null
    @Volatile private var discovered: List<String>? = null

    /** Last resort if /v1/models cannot be read. Models seen live on integrate.api.nvidia.com in Nov 2026. */
    private val fallback = listOf("nvidia/nemotron-3.5-lightning-30b-a3b", "nvidia/nemotron-3-ultra-550b-a55b")

    private val notChat = listOf(
        "embed", "rerank", "guard", "safety", "clip", "parse", "detector", "translate", "asr", "tts", "ocr", "reward",
        "diffusion", "image", "video", "retriev", "jailbreak", "topic-control", "vlm", "-vl", "vision", "audio",
        "speech", "fuyu", "kosmos", "paligemma", "neva", "deplot", "bge", "synthetic",
    )
    private val prefer = listOf("lightning", "nano", "instruct", "flash", "mini", "small", "gemma", "llama", "qwen", "mistral", "nemotron")
    private val huge = Regex("(\\d{3,})b")

    /** Keeps chat models only and puts fast, small ones first. */
    private fun rank(ids: List<String>): List<String> =
        ids.filter { id -> notChat.none { id.lowercase().contains(it) } }
            .sortedBy { id ->
                val l = id.lowercase()
                val p = prefer.indexOfFirst { l.contains(it) }.let { if (it < 0) prefer.size else it }
                val size = huge.find(l)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                p + if (size >= 200 || l.contains("ultra")) 100 else 0
            }

    /** Blocking network call: only run it on an IO thread. */
    private fun discoverModels(): List<String> {
        discovered?.let { return it }
        var result: List<String> = emptyList()
        try {
            val req = Request.Builder().url("$BASE/models").header("Authorization", "Bearer $key").get().build()
            http.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr: JSONArray = JSONObject(resp.body?.string().orEmpty()).optJSONArray("data") ?: JSONArray()
                    val ids = (0 until arr.length()).mapNotNull { i ->
                        arr.optJSONObject(i)?.takeIf { !it.isNull("id") }?.optString("id", "")?.takeIf { it.isNotBlank() }
                    }
                    result = rank(ids)
                    if (result.isNotEmpty()) discovered = result
                }
            }
        } catch (e: Exception) {
            // no list: the fallback models below are used instead
        }
        return result
    }

    private fun candidates(): List<String> {
        val list = LinkedHashSet<String>()
        working?.let { list.add(it) }
        if (configuredModel.isNotBlank()) list.add(configuredModel)
        list.addAll(discoverModels())
        list.addAll(fallback)
        return list.filter { it !in dead }.take(6)
    }

    /** Tries the candidate models in order; a retired one (410/404) is skipped and remembered as dead. */
    private suspend fun withModels(attempt: suspend (String) -> AiResult): AiResult {
        if (!isConfigured) return AiResult.Fail(AiError.NO_KEY)
        var last: AiResult = AiResult.Fail(AiError.MODEL_GONE)
        for (round in 0..1) {
            for (model in candidates()) {
                val r = attempt(model)
                if (r is AiResult.Fail && r.error == AiError.MODEL_GONE) {
                    dead.add(model)
                    last = r
                    continue
                }
                if (r is AiResult.Ok) working = model
                return r
            }
            discovered = null // everything we knew is dead: read the live list again once
        }
        return last
    }

    // ---------------------------------------------------------------- helpers

    /** Removes <think>...</think> (also an unfinished one while streaming) and stray markdown fences. */
    fun clean(s: String): String = s
        .replace(Regex("<think>[\\s\\S]*?</think>"), "")
        .replace(Regex("<think>[\\s\\S]*$"), "")
        .replace("```", "")
        .trim()

    /** org.json gives the text "null" for a JSON null on Android; this gives an empty string instead. */
    private fun JSONObject?.text(name: String): String =
        if (this == null || isNull(name)) "" else optString(name, "")

    private fun body(model: String, messages: List<AiMessage>, maxTokens: Int, temperature: Double, stream: Boolean): String {
        val arr = JSONArray()
        messages.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
        return JSONObject()
            .put("model", model)
            // extra room because some models spend tokens on hidden reasoning before the visible answer
            .put("max_tokens", maxTokens + 1200)
            .put("temperature", temperature)
            .put("stream", stream)
            .put("messages", arr)
            .toString()
    }

    private fun request(json: String, stream: Boolean): Request = Request.Builder()
        .url("$BASE/chat/completions")
        .header("Authorization", "Bearer $key")
        .header("Accept", if (stream) "text/event-stream" else "application/json")
        .post(json.toRequestBody("application/json".toMediaType()))
        .build()

    private fun httpError(model: String, code: Int, text: String): AiResult.Fail = when {
        code == 410 || code == 404 -> AiResult.Fail(AiError.MODEL_GONE, "$model: HTTP $code")
        code == 401 || code == 403 -> AiResult.Fail(AiError.BAD_KEY, "HTTP $code")
        code == 429 -> AiResult.Fail(AiError.RATE_LIMIT, "HTTP 429")
        code in 500..599 -> AiResult.Fail(AiError.SERVER, "$model: HTTP $code")
        else -> AiResult.Fail(AiError.OTHER, "$model: HTTP $code ${text.take(160)}")
    }

    private fun exceptionError(e: Exception): AiResult.Fail = when (e) {
        is UnknownHostException, is ConnectException -> AiResult.Fail(AiError.NO_INTERNET)
        is SocketTimeoutException -> AiResult.Fail(AiError.TIMEOUT)
        else -> AiResult.Fail(AiError.OTHER, e.message)
    }

    // ---------------------------------------------------------------- requests

    /** Whole answer at once. */
    suspend fun ask(system: String, user: String, maxTokens: Int = 700, temperature: Double = 0.4): AiResult =
        askMessages(listOf(AiMessage("system", system), AiMessage("user", user)), maxTokens, temperature)

    suspend fun askMessages(messages: List<AiMessage>, maxTokens: Int = 700, temperature: Double = 0.4): AiResult =
        withContext(Dispatchers.IO) {
            withModels { model ->
                try {
                    val call = http.newCall(request(body(model, messages, maxTokens, temperature, false), false))
                    val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { if (it != null) call.cancel() }
                    try {
                        call.execute().use { resp ->
                            val raw = resp.body?.string().orEmpty()
                            if (!resp.isSuccessful) {
                                httpError(model, resp.code, raw)
                            } else {
                                val msg = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                                val cleaned = clean(msg.text("content"))
                                if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY, model) else AiResult.Ok(cleaned)
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
        withModels { model ->
            try {
                val call = http.newCall(request(body(model, messages, maxTokens, temperature, true), true))
                val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { if (it != null) call.cancel() }
                try {
                    call.execute().use { resp ->
                        if (!resp.isSuccessful) {
                            httpError(model, resp.code, resp.body?.string().orEmpty())
                        } else {
                            val source = resp.body?.source()
                            val raw = StringBuilder()
                            if (source != null) {
                                while (true) {
                                    val line = source.readUtf8Line() ?: break
                                    if (!line.startsWith("data:")) continue
                                    val data = line.removePrefix("data:").trim()
                                    if (data == "[DONE]") break
                                    val delta = try {
                                        JSONObject(data).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta").text("content")
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    if (delta.isNotEmpty()) {
                                        raw.append(delta)
                                        onText(clean(raw.toString()))
                                    }
                                }
                            }
                            val cleaned = clean(raw.toString())
                            if (cleaned.isBlank()) AiResult.Fail(AiError.EMPTY, model) else AiResult.Ok(cleaned)
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
}
