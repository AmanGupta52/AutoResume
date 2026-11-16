package com.example.api

import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Request data structures using standard Moshi serialization
data class Part(val text: String? = null)
data class Content(val parts: List<Part>)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

data class Candidate(val content: Content)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
    }

    suspend fun generateSummary(jobTitle: String, skills: String, experience: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Passionate and results-driven $jobTitle with expertise in $skills. Experienced in executing successful projects and collaborating across teams to build highly performant, accessible applications. Eager to bring strong technical problem-solving capabilities to a progressive organizational team."
        }

        val prompt = "Generate a short, powerful, 3-sentence professional summary for a resume of a person who is a '$jobTitle', possesses skills: '$skills', and has the following experience: '$experience'."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert resume writer. Return only the generated summary and nothing else.")))
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "No response from AI."
        } catch (e: Exception) {
            "Professional resume summary crafted for a $jobTitle specializing in $skills to highlight key career achievements and academic strengths."
        }
    }

    // AI suggestions for bullet points
    suspend fun generateBulletPoints(role: String, company: String, details: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "• Spearheaded the development of core features for $role, improving customer engagement metrics by 15%.\n• Collaborated with cross-functional design and engineering teams to model interactive dashboards, boosting page load performance by 30%.\n• Implemented automated code validation and testing pipelines to decrease system bugs and maintenance efforts in $company."
        }

        val prompt = "Generate exactly 3 professional work experience achievement bullet points for a '$role' at '$company' based on these details: '$details'. Emphasize actions and quantifiable results/metrics using active verbs."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert resume career coach. Output bullet points starting with '•'. Return only the bullet points, nothing else.")))
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Error generating bullet points."
        } catch (e: Exception) {
            "• Developed responsive applications for $role at $company to optimize client-side workflows.\n• Participated in system architecture reviews and integrated secure cloud database endpoints, raising robustness."
        }
    }

    // AI suggestion for skills
    suspend fun generateSkillSuggestions(role: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Kotlin, Jetpack Compose, Android SDK, MVVM, Room Database, SQLite, Git, CI/CD, Retrofit, REST APIs, UI/UX Design, Figma, Problem Solving, Agile Development"
        }

        val prompt = "Suggest a comma-separated list of 12 highly relevant hard and soft skills for a target career role: '$role'."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "Return ONLY a comma-separated list of skills, nothing else. No extra text.")))
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "Kotlin, Android SDK, Compose"
        } catch (e: Exception) {
            "Kotlin, Android SDK, Room, Git, Figma, Communication, Technical Problem Solving"
        }
    }

    // AI ATS Resume Auto-Fix rewrite
    suspend fun autoFixResume(resumeText: String, targetJobDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Optimized with tailored keywords such as $targetJobDescription to align with candidate experience profile."
        }

        val prompt = "Rewrite or give concrete recommendations to optimize this resume summary/content to better match the target job description:\n\nResume content: $resumeText\n\nJob Description: $targetJobDescription"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert ATS optimizer. Provide clear, direct rewritten text or bullet suggestions to maximize keywords match.")))
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: "No response from AI."
        } catch (e: Exception) {
            "Failed to connect to AI server. Please verify your internet connection. (Fallback: Match job keywords in your Experience block)"
        }
    }

    // Extracts keywords for ATS scan from Job Description
    suspend fun extractKeywords(jobDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Project Management, Communication, Cloud Computing, Database Optimization, Secure Authorization, Agile, API Integration"
        }

        val prompt = "Extract exactly 8 important skills/keywords from this job description:\n\n$jobDescription"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "Return ONLY 8 comma-separated keywords, no introductory or concluding text.")))
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            "Collaboration, Technical Documentation, Code Quality, Cloud Deployments"
        }
    }

    // Cover letter. Returns null when the key is missing or the request fails (caller falls back to an offline draft).
    suspend fun generateCoverLetter(
        resumeText: String, jobDescription: String, role: String, company: String, manager: String, tone: String,
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext null
        val prompt = "Write a cover letter for the role '$role' at '${company.ifBlank { "the company" }}', addressed to '${manager.ifBlank { "Hiring Manager" }}'. " +
            "Tone: $tone. Length: 220-300 words, 3 short paragraphs plus sign-off.\n\nRESUME:\n$resumeText\n\nJOB DESCRIPTION:\n$jobDescription"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You write honest, specific cover letters. Use ONLY facts that appear in the resume; never invent employers, numbers or skills. Plain text, no markdown, no placeholders in brackets. Return only the letter."))),
        )
        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (e: Exception) {
            null
        }
    }
}
