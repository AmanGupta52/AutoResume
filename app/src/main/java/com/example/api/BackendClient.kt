package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class ApiResult {
    data class Ok(val json: JSONObject) : ApiResult()
    /** [status] is 0 when the server could not be reached at all. */
    data class Fail(val status: Int, val code: String, val message: String, val json: JSONObject? = null) : ApiResult() {
        val unauthorized: Boolean get() = status == 401 && code == "UNAUTHORIZED"
        val offline: Boolean get() = status == 0
    }
}

/**
 * Talks to the AutoResume backend (backend/ in this repo). The address comes from BACKEND_URL in `.env`
 * (GitHub secret/variable BACKEND_URL). Without it the app simply works offline, as before.
 */
object BackendClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    private val base: String get() = BuildConfig.BACKEND_URL.trim().trim('"', '\'', ' ').trimEnd('/')

    /** Full address of an API path, e.g. urlOf("/api/ai/chat"). */
    fun urlOf(path: String): String = "$base$path"

    val isConfigured: Boolean
        get() = base.startsWith("http") && !base.contains("your-backend") && !base.contains("MY_BACKEND")

    private fun builder(path: String, token: String?): Request.Builder =
        Request.Builder().url("$base$path")
            .header("Accept", "application/json")
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }

    suspend fun call(method: String, path: String, body: JSONObject? = null, token: String? = null): ApiResult {
        val payload: RequestBody? = when (method) {
            "GET" -> null
            else -> (body ?: JSONObject()).toString().toRequestBody(json)
        }
        return execute { builder(path, token).method(method, payload).build() }
    }

    /** Sends a file as the request body (used for the PDF), e.g. PUT /api/resumes/{id}/pdf. */
    suspend fun putBytes(path: String, bytes: ByteArray, token: String, contentType: String = "application/pdf"): ApiResult =
        execute { builder(path, token).put(bytes.toRequestBody(contentType.toMediaType())).build() }

    private suspend fun execute(makeRequest: () -> Request): ApiResult = withContext(Dispatchers.IO) {
        try {
            http.newCall(makeRequest()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val obj = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
                if (resp.isSuccessful) ApiResult.Ok(obj)
                else ApiResult.Fail(
                    resp.code,
                    if (obj.has("error")) obj.optString("error", "ERROR") else "ERROR",
                    if (obj.has("message")) obj.optString("message", "Something went wrong.") else "Something went wrong (HTTP ${resp.code}).",
                    obj,
                )
            }
        } catch (e: UnknownHostException) {
            ApiResult.Fail(0, "NETWORK", "No internet connection.")
        } catch (e: ConnectException) {
            ApiResult.Fail(0, "NETWORK", "Cannot reach the server.")
        } catch (e: SocketTimeoutException) {
            ApiResult.Fail(0, "NETWORK", "The server took too long to answer.")
        } catch (e: Exception) {
            ApiResult.Fail(0, "NETWORK", e.message ?: "Network error.")
        }
    }
}
