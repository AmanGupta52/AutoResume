package com.example

import com.example.ui.AtsAnalyzerEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class AtsAnalyzerEngineTest {

    @Test
    fun `analyzeText identifies matched and missing keywords accurately`() {
        val preset = AtsAnalyzerEngine.commonJobDescriptions.first { it.id == "android" }
        val userText = """
            Android Software Engineer | dev@example.com
            Summary: Built mobile apps with Kotlin, Jetpack Compose, and MVVM.
            Experience:
            • Engineered offline caching with Room and REST APIs, improving load speed by 30%.
            • Collaborated with Git code reviews.
            Education: B.S. Computer Science
        """.trimIndent()

        val result = AtsAnalyzerEngine.analyzeText(
            resumeText = userText,
            jobDescription = preset.description,
            presetKeywords = preset.keywords
        )

        assertTrue("Expected Kotlin to be matched", result.foundKeywords.any { it.equals("Kotlin", ignoreCase = true) })
        assertTrue("Expected Jetpack Compose to be matched", result.foundKeywords.any { it.equals("Jetpack Compose", ignoreCase = true) })
        assertTrue("Expected CI/CD to be missing", result.missingKeywords.any { it.equals("CI/CD", ignoreCase = true) })
        assertTrue("Expected score between 1 and 100", result.overallScore in 1..100)
        assertTrue("Expected actionable optimization suggestions", result.suggestions.isNotEmpty())
    }

    @Test
    fun `analyzeText scores higher when missing keywords and metrics are added`() {
        val preset = AtsAnalyzerEngine.commonJobDescriptions.first { it.id == "fullstack" }
        val weakText = "Worked on web projects and helped team."
        val strongText = """
            Full-Stack Developer | alex@example.com
            • Engineered TypeScript, React, and Node.js services with SQL and REST APIs, reducing latency by 35%.
            • Automated Docker, AWS, and CI/CD pipelines with Git and Automated Testing for 100k+ users.
            • Led System Design and Agile sprint delivery.
            Education: B.S. Software Engineering, GPA 3.9
        """.trimIndent()

        val weakResult = AtsAnalyzerEngine.analyzeText(weakText, preset.description, preset.keywords)
        val strongResult = AtsAnalyzerEngine.analyzeText(strongText, preset.description, preset.keywords)

        assertTrue(
            "Expected strong resume text (${strongResult.overallScore}) to score higher than weak text (${weakResult.overallScore})",
            strongResult.overallScore > weakResult.overallScore
        )
        assertTrue(
            "Expected weak text to produce multiple optimization suggestions",
            weakResult.suggestions.size >= 3
        )
    }
}
