package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Resume
import com.example.ui.locale.Text
import com.example.ui.theme.AppTheme
import java.util.Locale

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AtsAnalyzerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val resumes by viewModel.resumes.collectAsState()
    val isAnalyzing by viewModel.isAtsAnalyzing.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiFeedback by viewModel.aiMessageFeedback.collectAsState()
    val scanResult by viewModel.atsScanResult.collectAsState()
    val userInputText by viewModel.atsUserInputText.collectAsState()
    val targetJdInput by viewModel.atsJobDescription.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    val presets = AtsAnalyzerEngine.commonJobDescriptions
    var selectedPresetId by remember { mutableStateOf(presets.first().id) }
    var showResumePicker by remember { mutableStateOf(false) }

    // Seed a helpful default common Job Description on first visit if empty
    LaunchedEffect(Unit) {
        if (targetJdInput.isBlank()) {
            val defaultPreset = presets.first()
            selectedPresetId = defaultPreset.id
            viewModel.atsJobDescription.value = defaultPreset.description
        }
    }

    BackHandler {
        onNavigateBack()
    }

    val activePreset = presets.find { it.id == selectedPresetId }
    val wordCount = remember(userInputText) {
        userInputText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.background)
    ) {
        // Top Header Bar
        Surface(
            color = AppTheme.card,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ats_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppTheme.text
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ATS Resume Analyzer",
                        color = AppTheme.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Compare your text against job keywords",
                        color = AppTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Box {
                    TextButton(
                        onClick = { showResumePicker = true },
                        modifier = Modifier.testTag("ats_load_saved_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = AppTheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Draft", color = AppTheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showResumePicker,
                        onDismissRequest = { showResumePicker = false },
                        modifier = Modifier.background(AppTheme.card)
                    ) {
                        if (resumes.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No saved resumes yet", color = AppTheme.textSecondary) },
                                onClick = { showResumePicker = false }
                            )
                        } else {
                            resumes.forEach { resume ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(resume.name, color = AppTheme.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(resume.jobTitle.ifBlank { "Resume Draft" }, color = AppTheme.textSecondary, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.atsSelectedResumeId.value = resume.id
                                        viewModel.atsUserInputText.value = AtsAnalyzerEngine.resumeToPlainText(resume)
                                        showResumePicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1: USER RESUME TEXT INPUT
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppTheme.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. YOUR RESUME TEXT",
                            color = AppTheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppTheme.primary.copy(alpha = 0.12f))
                                    .clickable {
                                        viewModel.atsUserInputText.value =
                                            "Android Software Engineer | alex@example.com | github.com/alex\n\n" +
                                            "Summary:\nProduct-focused engineer building Kotlin and Jetpack Compose mobile apps with MVVM architecture.\n\n" +
                                            "Work Experience:\n" +
                                            "• Engineered Compose UI flows that improved onboarding conversion by 22%.\n" +
                                            "• Implemented offline-first Room Database caching and REST APIs, reducing load latency by 30%.\n" +
                                            "• Collaborated in Agile sprints with Git code reviews and unit testing.\n\n" +
                                            "Education:\nB.S. Computer Science, Stanford University (2019-2023)"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("ats_insert_sample_button")
                            ) {
                                Text("Sample Text", color = AppTheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (userInputText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppTheme.background)
                                        .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.atsUserInputText.value = "" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Clear", color = AppTheme.textSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = userInputText,
                        onValueChange = { viewModel.atsUserInputText.value = it },
                        placeholder = {
                            Text(
                                text = "Paste or type your resume summary, work experience bullets, skills, and education here...",
                                color = AppTheme.textSecondary.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        },
                        colors = editorOutlinedTextFieldColors(AppTheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ats_resume_text_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$wordCount words • ${userInputText.length} chars",
                            color = AppTheme.textSecondary,
                            fontSize = 11.sp
                        )
                        if (resumes.isNotEmpty() && userInputText.isBlank()) {
                            Text(
                                text = "Tip: Tap 'Load Draft' above to import a saved resume",
                                color = AppTheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // STEP 2: COMMON JOB DESCRIPTION SELECTOR & COMPARISON INPUT
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppTheme.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. COMMON JOB DESCRIPTION BENCHMARK",
                        color = AppTheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = selectedPresetId == preset.id &&
                                targetJdInput.trim() == preset.description.trim()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AppTheme.accent else AppTheme.background)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AppTheme.accent else AppTheme.border,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedPresetId = preset.id
                                        viewModel.atsJobDescription.value = preset.description
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("ats_preset_${preset.id}")
                            ) {
                                Text(
                                    text = preset.label,
                                    color = if (isSelected) Color.White else AppTheme.text,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetJdInput,
                        onValueChange = { viewModel.atsJobDescription.value = it },
                        label = { Text("Job Description Keywords / Text", fontSize = 12.sp) },
                        placeholder = {
                            Text(
                                text = "Select a common role above or paste a custom job description...",
                                color = AppTheme.textSecondary.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = if (targetJdInput.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.atsJobDescription.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear job description",
                                        tint = AppTheme.textSecondary
                                    )
                                }
                            }
                        } else null,
                        colors = editorOutlinedTextFieldColors(AppTheme.accent),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ats_jd_input")
                    )
                }
            }

            // ANALYZE CTA
            Button(
                onClick = {
                    viewModel.analyzeResumeTextAgainstJd(
                        resumeText = userInputText,
                        jobDescriptionText = targetJdInput,
                        presetKeywords = activePreset?.keywords ?: emptyList()
                    )
                },
                enabled = userInputText.isNotBlank() && targetJdInput.isNotBlank() && !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ats_scan_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing Keywords & Score...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze ATS Compatibility", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // AI Loading Banner
            AnimatedVisibility(visible = isAiLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9500).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFFFF9500))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = aiFeedback.ifBlank { "Optimizing resume text..." },
                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // RESULTS PANEL
            scanResult?.let { res ->
                val animatedScore by animateFloatAsState(
                    targetValue = res.overallScore.toFloat(),
                    animationSpec = tween(durationMillis = 600),
                    label = "ats_score_anim"
                )
                val meterColor = when {
                    res.overallScore >= 75 -> Color(0xFF30D158)
                    res.overallScore >= 50 -> Color(0xFFFF9500)
                    else -> Color(0xFFEF4444)
                }
                val matchVerdict = when {
                    res.overallScore >= 75 -> "Strong ATS Match"
                    res.overallScore >= 50 -> "Moderate Match — Optimization Recommended"
                    else -> "Low Match — Add Missing Job Keywords"
                }

                // Score Overview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ats_score_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OVERALL ATS COMPATIBILITY SCORE",
                            color = AppTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                            Canvas(modifier = Modifier.size(110.dp)) {
                                drawArc(
                                    color = if (isDark) Color(0xFF2A2D34) else AppTheme.shadowDark.copy(alpha = 0.25f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx())
                                )
                                drawArc(
                                    color = meterColor,
                                    startAngle = -90f,
                                    sweepAngle = (animatedScore * 3.6f),
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${res.overallScore}%",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = meterColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = matchVerdict,
                            color = AppTheme.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${res.foundKeywords.size} of ${res.foundKeywords.size + res.missingKeywords.size} target keywords found in your text",
                            color = AppTheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Breakdown Bars Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SCORE BREAKDOWN",
                            color = AppTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        AtsBreakdownRow("Keywords Match Rate", res.keywordsMatchPercent, AppTheme.primary)
                        AtsBreakdownRow("Section & Content Completeness", res.completenessScore, Color(0xFF30D158))
                        AtsBreakdownRow("Readability, Bullets & Metrics", res.readabilityScore, Color(0xFFFF9500))
                        AtsBreakdownRow("Action & Contact Signals", res.contactInfoScore, Color(0xFF06B6D4))
                    }
                }

                // Keywords Comparison Matrix
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "KEYWORD COMPARISON MATRIX",
                            color = AppTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Matched Keywords (${res.foundKeywords.size})",
                                color = Color(0xFF16A34A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (res.foundKeywords.isEmpty()) {
                                Text(
                                    text = "No target keywords detected yet in your input text.",
                                    color = AppTheme.textSecondary,
                                    fontSize = 12.sp
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    res.foundKeywords.forEach { kw ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF30D158).copy(alpha = 0.14f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(kw, color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Missing Keywords (${res.missingKeywords.size}) — Tap to insert",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (res.missingKeywords.isEmpty()) {
                                Text(
                                    text = "All extracted job description keywords are covered!",
                                    color = Color(0xFF16A34A),
                                    fontSize = 12.sp
                                )
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    res.missingKeywords.forEach { kw ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val current = viewModel.atsUserInputText.value.trimEnd()
                                                    val appended = if (current.isEmpty()) kw else "$current\n• Experienced with $kw"
                                                    viewModel.atsUserInputText.value = appended
                                                    viewModel.analyzeResumeTextAgainstJd(
                                                        resumeText = appended,
                                                        jobDescriptionText = targetJdInput,
                                                        presetKeywords = activePreset?.keywords ?: emptyList()
                                                    )
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                .testTag("missing_kw_${kw.lowercase(Locale.US).replace(" ", "_")}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add $kw",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(kw, color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Optimization Suggestions Card List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "OPTIMIZATION SUGGESTIONS (${res.suggestions.size})",
                        color = AppTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )

                    res.suggestions.forEachIndexed { idx, suggestion ->
                        val (sevBg, sevText) = when (suggestion.severity.lowercase(Locale.US)) {
                            "high" -> Color(0xFFEF4444).copy(alpha = 0.14f) to Color(0xFFDC2626)
                            "medium" -> Color(0xFFFF9500).copy(alpha = 0.16f) to Color(0xFFD97706)
                            else -> AppTheme.primary.copy(alpha = 0.14f) to Color(0xFF2563EB)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AppTheme.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ats_suggestion_card_$idx")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (suggestion.severity.equals("High", ignoreCase = true)) {
                                                Icons.Default.Warning
                                            } else {
                                                Icons.Default.Lightbulb
                                            },
                                            contentDescription = null,
                                            tint = sevText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = suggestion.title,
                                            color = AppTheme.text,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(sevBg)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = suggestion.severity.uppercase(Locale.US),
                                            color = sevText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = suggestion.instruction,
                                    color = AppTheme.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // AI Auto-Optimize Action Button
                Button(
                    onClick = {
                        viewModel.optimizeUserTextWithAi(
                            userText = userInputText,
                            jobDescription = targetJdInput,
                            presetKeywords = activePreset?.keywords ?: emptyList()
                        )
                    },
                    enabled = userInputText.isNotBlank() && !isAiLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ats_ai_fix_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rewrite & Optimize Text for ATS Match", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
