package com.example.api

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI requests through NVIDIA's hosted, OpenAI-compatible API (https://build.nvidia.com).
 * Key and model come from `.env` (NVIDIA_API_KEY, NVIDIA_MODEL) at build time.
 * Every function returns null when the key is missing or the request fails, so the UI can say so
 * instead of inserting made-up text into a resume.
 */
object AiClient {
    private const val URL = "https://integrate.api.nvidia.com/v1/chat/completions"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val key: String get() = BuildConfig.NVIDIA_API_KEY.trim()
    private val model: String get() = BuildConfig.NVIDIA_MODEL.trim().ifBlank { "meta/llama-3.3-70b-instruct" }

    val isConfigured: Boolean get() = key.isNotBlank() && key != "MY_NVIDIA_API_KEY"

    /** Reasoning models may print <think>...</think> first; strip it, plus stray markdown fences. */
    private fun clean(s: String): String = s
        .replace(Regex("<think>[\\s\\S]*?</think>"), "")
        .replace("```", "")
        .trim()

    private suspend fun chat(system: String, user: String, maxTokens: Int = 700, temperature: Double = 0.4): String? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext null
            try {
                val body = JSONObject()
                    .put("model", model)
                    .put("max_tokens", maxTokens)
                    .put("temperature", temperature)
                    .put("stream", false)
                    .put("messages", JSONArray()
                        .put(JSONObject().put("role", "system").put("content", system))
                        .put(JSONObject().put("role", "user").put("content", user)))
                    .toString()
                val req = Request.Builder()
                    .url(URL)
                    .header("Authorization", "Bearer $key")
                    .header("Accept", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val text = JSONObject(resp.body?.string().orEmpty())
                        .optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("message")?.optString("content").orEmpty()
                    clean(text).ifBlank { null }
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun generateSummary(jobTitle: String, skills: String, experience: String): String? = chat(
        system = "You are an expert resume writer. Write a 3-sentence professional summary in first person without pronouns like 'I'. " +
            "Use ONLY the facts given; never invent employers, numbers or skills. Plain text, no markdown. Return only the summary.",
        user = "Job title: $jobTitle\nSkills: ${skills.ifBlank { "not given" }}\nExperience: ${experience.ifBlank { "not given" }}",
        maxTokens = 300,
    )

    suspend fun generateBulletPoints(role: String, company: String, details: String): String? = chat(
        system = "You are a resume career coach. Write exactly 3 achievement bullet points, each starting with '• ' and an active verb. " +
            "Keep to the facts supplied; if no numbers are given do not invent any. Max 22 words each. Return only the bullets.",
        user = "Role: $role\nCompany: $company\nWhat the person did: ${details.ifBlank { "not given" }}",
        maxTokens = 350,
    )

    suspend fun generateSkillSuggestions(role: String): String? = chat(
        system = "Return ONLY a comma-separated list of 12 relevant hard and soft skills for the role. No numbering, no extra text.",
        user = "Target role: $role",
        maxTokens = 150,
    )

    suspend fun autoFixResume(resumeText: String, targetJobDescription: String): String? = chat(
        system = "You are an ATS optimisation expert. Compare the resume with the job description and give: " +
            "1) a rewritten professional summary, 2) up to 5 concrete bullet rewrites that naturally include missing keywords. " +
            "Only use facts present in the resume. Plain text, no markdown.",
        user = "RESUME:\n$resumeText\n\nJOB DESCRIPTION:\n$targetJobDescription",
        maxTokens = 900,
    )

    suspend fun generateCoverLetter(
        resumeText: String, jobDescription: String, role: String, company: String, manager: String, tone: String,
    ): String? = chat(
        system = "You write honest, specific cover letters. Use ONLY facts that appear in the resume; never invent employers, numbers or skills. " +
            "Plain text, no markdown, no placeholders in brackets. Return only the letter.",
        user = "Write a cover letter for the role '$role' at '${company.ifBlank { "the company" }}', addressed to '${manager.ifBlank { "Hiring Manager" }}'. " +
            "Tone: $tone. Length: 220-300 words, 3 short paragraphs plus sign-off.\n\nRESUME:\n$resumeText\n\nJOB DESCRIPTION:\n$jobDescription",
        maxTokens = 700,
        temperature = 0.5,
    )
}
