package com.example.ui

import com.example.data.Resume
import java.util.Locale

data class AtsScanData(
    val overallScore: Int,
    val keywordsMatchPercent: Int,
    val formattingScore: Int,
    val completenessScore: Int,
    val readabilityScore: Int,
    val contactInfoScore: Int,
    val foundKeywords: List<String>,
    val missingKeywords: List<String>,
    val suggestions: List<AtsSuggestion>
)

data class AtsSuggestion(
    val title: String,
    val severity: String, // High, Medium, Low
    val instruction: String
)

data class CommonJobDescriptionPreset(
    val id: String,
    val label: String,
    val description: String,
    val keywords: List<String>
)

object AtsAnalyzerEngine {

    val commonJobDescriptions: List<CommonJobDescriptionPreset> = listOf(
        CommonJobDescriptionPreset(
            id = "android",
            label = "Android Engineer",
            description = "Seeking an Android Software Engineer experienced with Kotlin, Jetpack Compose, MVVM architecture, Coroutines, Room Database, REST APIs, Git, Unit Testing, CI/CD pipelines, and Agile delivery to build high-performance mobile apps.",
            keywords = listOf(
                "Kotlin", "Jetpack Compose", "MVVM", "Coroutines", "Room",
                "REST APIs", "Git", "Unit Testing", "CI/CD", "Agile",
                "Android SDK", "Performance"
            )
        ),
        CommonJobDescriptionPreset(
            id = "fullstack",
            label = "Full-Stack Dev",
            description = "Looking for a Full-Stack Developer proficient in TypeScript, React, Node.js, SQL, REST APIs, Docker, AWS, Git, Automated Testing, CI/CD, and System Design to deliver scalable web applications.",
            keywords = listOf(
                "TypeScript", "React", "Node.js", "SQL", "REST APIs",
                "Docker", "AWS", "Git", "Testing", "CI/CD",
                "System Design", "Agile"
            )
        ),
        CommonJobDescriptionPreset(
            id = "pm",
            label = "Product Manager",
            description = "Hiring a Product Manager skilled in Product Roadmap, Stakeholder Management, Agile Scrum, KPIs, User Research, A/B Testing, Prioritization, Cross-functional Leadership, Analytics, SQL, and Go-to-Market strategy.",
            keywords = listOf(
                "Roadmap", "Stakeholder Management", "Agile", "KPIs", "User Research",
                "A/B Testing", "Prioritization", "Cross-functional", "Analytics", "SQL",
                "Go-to-Market", "Strategy"
            )
        ),
        CommonJobDescriptionPreset(
            id = "data",
            label = "Data Analyst",
            description = "Seeking a Data Analyst with expertise in SQL, Python, Tableau, Power BI, Data Visualization, ETL pipelines, Statistics, Excel, A/B Testing, Forecasting, and Stakeholder Reporting.",
            keywords = listOf(
                "SQL", "Python", "Tableau", "Power BI", "Data Visualization",
                "ETL", "Statistics", "Excel", "A/B Testing", "Forecasting",
                "Reporting", "Analytics"
            )
        ),
        CommonJobDescriptionPreset(
            id = "ux",
            label = "UX Designer",
            description = "Looking for a UI/UX Designer experienced in Figma, Design Systems, Prototyping, User Research, Wireframing, Accessibility, Usability Testing, Interaction Design, Typography, and Developer Handoff.",
            keywords = listOf(
                "Figma", "Design Systems", "Prototyping", "User Research", "Wireframing",
                "Accessibility", "Usability Testing", "Interaction Design", "Typography",
                "Responsive Design", "Collaboration", "Handoff"
            )
        )
    )

    private val knownSkillDictionary = listOf(
        "Kotlin", "Java", "Python", "TypeScript", "JavaScript", "Swift", "C++", "Go",
        "Jetpack Compose", "Android SDK", "MVVM", "Clean Architecture", "Coroutines", "Flow",
        "Room", "SQLite", "SQL", "PostgreSQL", "MySQL", "MongoDB", "Firebase", "Firestore",
        "React", "Node.js", "Next.js", "GraphQL", "REST APIs", "Retrofit", "Ktor",
        "Docker", "Kubernetes", "AWS", "GCP", "Azure", "CI/CD", "Git", "GitHub",
        "Unit Testing", "JUnit", "Robolectric", "Automated Testing", "TDD",
        "Agile", "Scrum", "Jira", "Code Review", "System Design", "Microservices",
        "Performance", "Optimization", "Security", "Authentication", "OAuth",
        "Roadmap", "Stakeholder Management", "KPIs", "OKRs", "User Research",
        "A/B Testing", "Prioritization", "Cross-functional", "Analytics", "Go-to-Market",
        "Tableau", "Power BI", "Data Visualization", "ETL", "Statistics", "Excel", "Forecasting",
        "Figma", "Design Systems", "Prototyping", "Wireframing", "Accessibility",
        "Usability Testing", "Interaction Design", "Typography", "Responsive Design",
        "Leadership", "Communication", "Mentoring", "Collaboration", "Problem Solving"
    )

    private val actionVerbs = listOf(
        "led", "built", "engineered", "developed", "designed", "architected", "implemented",
        "improved", "reduced", "increased", "optimized", "launched", "delivered", "managed",
        "created", "automated", "spearheaded", "collaborated", "scaled", "streamlined", "tested"
    )

    fun resumeToPlainText(resume: Resume): String {
        val parts = mutableListOf<String>()
        if (resume.fullName.isNotBlank() || resume.jobTitle.isNotBlank()) {
            parts.add("${resume.fullName} — ${resume.jobTitle}".trim(' ', '—'))
        }
        val contacts = listOf(resume.email, resume.phone, resume.location).filter { it.isNotBlank() }
        if (contacts.isNotEmpty()) {
            parts.add(contacts.joinToString(" | "))
        }
        if (resume.summary.isNotBlank()) {
            parts.add("Summary:\n${resume.summary}")
        }
        if (resume.experience.isNotEmpty()) {
            val expBlock = resume.experience.joinToString("\n\n") { exp ->
                val dates = listOf(exp.startDate, if (exp.isCurrent) "Present" else exp.endDate)
                    .filter { it.isNotBlank() }
                    .joinToString(" - ")
                "${exp.role} at ${exp.company} ($dates)\n${exp.description}".trim()
            }
            parts.add("Work Experience:\n$expBlock")
        }
        if (resume.education.isNotEmpty()) {
            val eduBlock = resume.education.joinToString("\n") { edu ->
                "${edu.degree}, ${edu.institution} (${edu.startYear}-${edu.endYear}) ${edu.grade}\n${edu.achievements}".trim()
            }
            parts.add("Education:\n$eduBlock")
        }
        if (resume.skills.isNotEmpty()) {
            parts.add("Skills:\n${resume.skills.joinToString(", ") { it.name }}")
        }
        if (resume.projects.isNotEmpty()) {
            val projBlock = resume.projects.joinToString("\n") { "${it.title} (${it.techStack}): ${it.description}" }
            parts.add("Projects:\n$projBlock")
        }
        return parts.joinToString("\n\n").trim()
    }

    fun extractKeywordsFromText(
        jobDescription: String,
        presetKeywords: List<String> = emptyList(),
        extraAiKeywords: List<String> = emptyList()
    ): List<String> {
        val jdLower = jobDescription.lowercase(Locale.US)
        val collected = LinkedHashMap<String, String>()

        fun addKeyword(raw: String) {
            val cleaned = raw.trim().trimEnd('.', ',', ';', ':')
            if (cleaned.length >= 2) {
                val key = cleaned.lowercase(Locale.US)
                if (!collected.containsKey(key)) {
                    collected[key] = cleaned
                }
            }
        }

        // 1. Add preset keywords that appear in the current JD (or all preset keywords if JD matches preset)
        presetKeywords.forEach { kw ->
            if (containsKeyword(jdLower, kw)) {
                addKeyword(kw)
            }
        }

        // 2. Scan known skill dictionary against the JD text
        knownSkillDictionary.forEach { skill ->
            if (containsKeyword(jdLower, skill)) {
                addKeyword(skill)
            }
        }

        // 3. Add any AI-extracted keywords that genuinely appear in the JD
        extraAiKeywords.forEach { kw ->
            if (containsKeyword(jdLower, kw)) {
                addKeyword(kw)
            }
        }

        // 4. Fallback token extraction for domain-specific custom JDs
        if (collected.size < 6) {
            val stopWords = setOf(
                "with", "from", "that", "this", "have", "will", "your", "team", "work",
                "experience", "looking", "seeking", "hiring", "role", "build", "using",
                "strong", "skills", "ability", "required", "preferred", "years", "including",
                "across", "must", "good", "great", "candidate", "position", "company", "about"
            )
            val tokens = Regex("[A-Za-z][A-Za-z0-9+#./-]{2,}")
                .findAll(jobDescription)
                .map { it.value.trimEnd('.', ',', ';', ':') }
                .filter { token ->
                    val lower = token.lowercase(Locale.US)
                    lower !in stopWords && (token.any { it.isUpperCase() } || lower.length >= 6)
                }
                .distinctBy { it.lowercase(Locale.US) }
                .take(12)
            tokens.forEach { addKeyword(it) }
        }

        if (collected.isEmpty() && presetKeywords.isNotEmpty()) {
            presetKeywords.forEach { addKeyword(it) }
        }

        return collected.values.take(14)
    }

    fun analyzeText(
        resumeText: String,
        jobDescription: String,
        presetKeywords: List<String> = emptyList(),
        extraAiKeywords: List<String> = emptyList()
    ): AtsScanData {
        val keywords = extractKeywordsFromText(jobDescription, presetKeywords, extraAiKeywords)
        val resumeLower = resumeText.lowercase(Locale.US)

        val found = mutableListOf<String>()
        val missing = mutableListOf<String>()

        keywords.forEach { kw ->
            if (containsKeyword(resumeLower, kw)) {
                found.add(kw)
            } else {
                missing.add(kw)
            }
        }

        val totalKeywords = keywords.size.coerceAtLeast(1)
        val keywordMatchPercent = ((found.size.toFloat() / totalKeywords) * 100f).toInt().coerceIn(0, 100)

        // Structure & Completeness checks
        val hasExperienceSection = listOf("experience", "engineer", "developer", "manager", "analyst", "designer", "worked", "company")
            .any { resumeLower.contains(it) }
        val hasEducationSection = listOf("education", "university", "college", "b.s.", "b.a.", "m.s.", "degree", "gpa", "cgpa", "bachelor", "master")
            .any { resumeLower.contains(it) }
        val hasSkillsSection = listOf("skills", "technologies", "tools", "proficient", "stack")
            .any { resumeLower.contains(it) } || found.size >= 3
        val hasContactSignals = resumeLower.contains("@") ||
            Regex("\\d{3}[-.\\s]?\\d{3}").containsMatchIn(resumeLower) ||
            resumeLower.contains("linkedin") ||
            resumeLower.contains("github")

        val completenessScore = (
            (if (hasExperienceSection) 35 else 10) +
                (if (hasEducationSection) 25 else 5) +
                (if (hasSkillsSection) 25 else 5) +
                (if (resumeText.trim().length >= 120) 15 else 5)
            ).coerceIn(0, 100)

        // Readability & Impact Metrics checks
        val wordCount = resumeText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val bulletLines = resumeText.lines().count { line ->
            val t = line.trimStart()
            t.startsWith("•") || t.startsWith("-") || t.startsWith("*")
        }
        val metricMatches = Regex("(\\d+%|\\$\\d+|\\d+x|\\d+\\+\\s|\\b\\d{2,}\\b)").findAll(resumeText).count()
        val actionVerbCount = actionVerbs.count { verb ->
            Regex("\\b${Regex.escape(verb)}\\b").containsMatchIn(resumeLower)
        }

        val lengthPoints = when {
            wordCount >= 120 -> 35
            wordCount >= 60 -> 25
            wordCount >= 25 -> 15
            else -> 5
        }
        val bulletPointsScore = when {
            bulletLines >= 3 -> 25
            bulletLines >= 1 -> 15
            else -> 5
        }
        val metricPoints = when {
            metricMatches >= 3 -> 25
            metricMatches >= 1 -> 15
            else -> 0
        }
        val verbPoints = when {
            actionVerbCount >= 3 -> 15
            actionVerbCount >= 1 -> 10
            else -> 0
        }
        val readabilityScore = (lengthPoints + bulletPointsScore + metricPoints + verbPoints).coerceIn(0, 100)

        val contactInfoScore = when {
            hasContactSignals && actionVerbCount >= 2 -> 95
            hasContactSignals || actionVerbCount >= 2 -> 75
            else -> 45
        }

        val overallScore = (
            keywordMatchPercent * 0.55f +
                completenessScore * 0.20f +
                readabilityScore * 0.25f
            ).toInt().coerceIn(0, 100)

        val suggestions = buildSuggestions(
            wordCount = wordCount,
            missingKeywords = missing,
            metricMatches = metricMatches,
            bulletLines = bulletLines,
            actionVerbCount = actionVerbCount,
            hasEducationSection = hasEducationSection,
            hasContactSignals = hasContactSignals
        )

        return AtsScanData(
            overallScore = overallScore,
            keywordsMatchPercent = keywordMatchPercent,
            formattingScore = readabilityScore,
            completenessScore = completenessScore,
            readabilityScore = readabilityScore,
            contactInfoScore = contactInfoScore,
            foundKeywords = found,
            missingKeywords = missing,
            suggestions = suggestions
        )
    }

    private fun buildSuggestions(
        wordCount: Int,
        missingKeywords: List<String>,
        metricMatches: Int,
        bulletLines: Int,
        actionVerbCount: Int,
        hasEducationSection: Boolean,
        hasContactSignals: Boolean
    ): List<AtsSuggestion> {
        val list = mutableListOf<AtsSuggestion>()

        if (missingKeywords.isNotEmpty()) {
            val topMissing = missingKeywords.take(5).joinToString(", ")
            list.add(
                AtsSuggestion(
                    title = "Add ${missingKeywords.size} missing job-description keywords",
                    severity = if (missingKeywords.size >= 4) "High" else "Medium",
                    instruction = "Naturally weave in high-signal terms from the job description: $topMissing. Tap any missing keyword chip below to insert it into your draft."
                )
            )
        }

        if (metricMatches < 2) {
            list.add(
                AtsSuggestion(
                    title = "Quantify achievements with numbers or percentages",
                    severity = "High",
                    instruction = "ATS reviewers prioritize measurable impact. Replace generic duties with outcomes like 'Reduced API latency by 28%' or 'Supported 50k+ monthly active users'."
                )
            )
        }

        if (bulletLines < 2) {
            list.add(
                AtsSuggestion(
                    title = "Structure experience using concise bullet points",
                    severity = "Medium",
                    instruction = "Start achievement lines with '•' or '-' so automated parsers and recruiters can scan your contributions quickly."
                )
            )
        }

        if (actionVerbCount < 2) {
            list.add(
                AtsSuggestion(
                    title = "Lead sentences with strong action verbs",
                    severity = "Medium",
                    instruction = "Begin bullets with verbs like Engineered, Architected, Led, Optimized, or Delivered instead of passive phrasing."
                )
            )
        }

        if (wordCount < 60) {
            list.add(
                AtsSuggestion(
                    title = "Expand resume content for fuller keyword coverage",
                    severity = "High",
                    instruction = "Your input currently has $wordCount words. Add your core skills, 1-2 work experience entries, and education details (aim for 120+ words)."
                )
            )
        }

        if (!hasEducationSection) {
            list.add(
                AtsSuggestion(
                    title = "Include education credentials",
                    severity = "Low",
                    instruction = "Add your degree, institution, and graduation year so education filters match your profile."
                )
            )
        }

        if (!hasContactSignals) {
            list.add(
                AtsSuggestion(
                    title = "Add email or portfolio/GitHub handle",
                    severity = "Low",
                    instruction = "Include at least an email address or LinkedIn/GitHub link when running a full-resume ATS check."
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                AtsSuggestion(
                    title = "Strong keyword & structure alignment",
                    severity = "Low",
                    instruction = "Your text already covers the core job description keywords, quantified metrics, and structured formatting. Tailor your top summary line to mirror the exact role title."
                )
            )
        }

        return list
    }

    private fun containsKeyword(haystackLower: String, keyword: String): Boolean {
        val kwLower = keyword.trim().lowercase(Locale.US)
        if (kwLower.isEmpty()) return false
        val escaped = Regex.escape(kwLower)
        // Use alphanumeric boundaries so e.g. "Git" doesn't match "Digital" and "Go" doesn't match "Google"
        val pattern = Regex("(?<![a-z0-9])$escaped(?![a-z0-9])")
        return pattern.containsMatchIn(haystackLower)
    }
}

