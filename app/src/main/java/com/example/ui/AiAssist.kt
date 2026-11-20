package com.example.ui

import com.example.api.AiError
import com.example.data.Resume
import java.util.Calendar
import java.util.Locale

/** Names of the editor tabs, in pager order. The AI is told which one the person is on. */
val EditorTabNames = listOf("Basics", "Summary", "Experience", "Education", "Skills", "Projects", "More", "Design", "Check")

enum class ProposalKind { SUMMARY, BULLETS, SKILLS, ADVICE }

/** Quick rewrite choices shown in the preview sheet. */
enum class Tweak(val label: String, val instruction: String) {
    SHORTER("Shorter", "Make it clearly shorter and keep only the strongest facts."),
    STRONGER("Stronger", "Make it stronger and more confident: sharper action verbs, clearer results. Add no new facts."),
    FORMAL("More formal", "Use a more formal, professional tone."),
    NUMBERS(
        "Add numbers",
        "Where a number would make a point stronger but the resume does not give one, write a placeholder in square brackets, " +
            "for example [X%] or [N customers]. Never write a real number that is not in the resume.",
    ),
    YEARS("Add years", "Mention the total years of experience given in the context, and nothing else new."),
}

data class AiRequest(val kind: ProposalKind, val index: Int = -1, val tweak: Tweak? = null)

/** What the AI wrote, waiting for the person to accept it. Nothing changes in the resume until then. */
data class AiProposal(
    val request: AiRequest,
    val title: String,
    val before: String,
    val text: String = "",
    val base: String = "", // the draft the AI was asked to rewrite (empty when writing from scratch)
    val loading: Boolean = true,
    val error: AiError? = null,
    val errorDetail: String? = null,
)

/** Everything the AI is told before every request, so it always knows the topic. */
object AiContext {
    private val focus = listOf(
        "Basics" to "contact details, links and the job title",
        "Summary" to "the professional summary (2-4 sentences)",
        "Experience" to "work achievements: action verbs, results and numbers",
        "Education" to "schools, degrees and dates",
        "Skills" to "the skills list only",
        "Projects" to "project descriptions and tools used",
        "More" to "certifications, languages, awards and interests",
        "Design" to "template, fonts and layout",
        "Check" to "fixing the problems found in the writing check",
        "ATS" to "matching the resume to a job description",
        "Guided" to "building the first version of the resume step by step",
    ).toMap()

    fun resumeText(r: Resume): String {
        val sb = StringBuilder()
        sb.append("NAME: ${r.fullName} | TITLE: ${r.jobTitle}\n")
        listOf(r.email, r.phone, r.location).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
            ?.let { sb.append("CONTACT: ${it.joinToString(" | ")}\n") }
        sb.append("SUMMARY: ${r.summary.ifBlank { "(empty)" }}\n")
        r.experience.forEachIndexed { i, e ->
            val dates = listOf(e.startDate, if (e.isCurrent) "Present" else e.endDate).filter { it.isNotBlank() }.joinToString(" - ")
            sb.append("JOB ${i + 1}: ${e.role} at ${e.company} ($dates)\n${e.description.ifBlank { "(no achievements yet)" }}\n")
        }
        r.education.forEachIndexed { i, e ->
            sb.append("EDUCATION ${i + 1}: ${e.degree}, ${e.institution} (${e.startYear}-${e.endYear}) ${e.grade}\n")
        }
        if (r.skills.isNotEmpty()) sb.append("SKILLS: ${r.skills.joinToString(", ") { it.name }}\n")
        r.projects.forEachIndexed { i, p -> sb.append("PROJECT ${i + 1}: ${p.title} (${p.techStack}): ${p.description}\n") }
        if (r.certifications.isNotEmpty()) sb.append("CERTIFICATIONS: ${r.certifications.joinToString(", ") { it.name }}\n")
        if (r.languages.isNotEmpty()) sb.append("LANGUAGES: ${r.languages.joinToString(", ") { it.name }}\n")
        if (r.achievements.isNotBlank()) sb.append("OTHER: ${r.achievements}\n")
        return sb.toString().trim().take(6000)
    }

    /** The context package: current tab, target job, what is missing, whole resume. */
    fun block(tab: String, r: Resume): String {
        val target = TargetJob.parse(r)
        val years = ResumeCoach.yearsOfExperience(r)
        val missing = ResumeCoach.checks(r).filter { !it.done }.map { it.label }
        return buildString {
            append("CURRENT SECTION: $tab (focus: ${focus[tab] ?: "the resume"})\n")
            append("TARGET JOB: ${target.title.ifBlank { r.jobTitle.ifBlank { "not set" } }}\n")
            if (target.description.isNotBlank()) append("JOB DESCRIPTION:\n${target.description.take(2500)}\n")
            if (years != null) append("YEARS OF EXPERIENCE (counted from the dates): about $years\n")
            append("STILL MISSING: ${if (missing.isEmpty()) "nothing important" else missing.joinToString("; ")}\n")
            append("\nRESUME:\n${resumeText(r)}")
        }
    }
}

/** Builds the prompts. Facts-only rules are in every system prompt. */
object AiPrompts {
    private const val RULES = "Use ONLY facts that appear in the resume. Never invent employers, numbers, dates, years or skills. " +
        "Plain text, no markdown. Return only the requested text, with no introduction."

    fun system(req: AiRequest): String = when (req.kind) {
        ProposalKind.SUMMARY ->
            "You are an expert resume writer. Write a professional summary of about 3 sentences, without the pronoun 'I'. " +
                "Tailor the wording to the target job when one is given. $RULES"
        ProposalKind.BULLETS ->
            "You are a resume career coach. Write 3 to 5 achievement bullets for ONE job. Each bullet starts with '• ' and an active verb, " +
                "max 22 words. Match the target job when given. If the person gave no numbers, do not invent any. $RULES"
        ProposalKind.SKILLS ->
            "Return ONLY a comma-separated list of 8 relevant skills for the target job. Prefer skills the resume already hints at " +
                "and keywords from the job description. Do not repeat skills already listed. No numbering, no extra text."
        ProposalKind.ADVICE ->
            "You are a resume coach. For each problem listed, write the exact improved text the person can paste in. " +
                "Number the answers 1, 2, 3. Where a number is missing write a placeholder like [X%] for the person to fill in. $RULES"
    }

    fun user(req: AiRequest, r: Resume, tab: String, current: String, problems: String = ""): String {
        val ctx = AiContext.block(tab, r)
        val task = when (req.kind) {
            ProposalKind.SUMMARY -> "Task: write the professional summary."
            ProposalKind.BULLETS -> {
                val e = r.experience.getOrNull(req.index)
                "Task: write the bullets for JOB ${req.index + 1}" + (e?.let { " (${it.role} at ${it.company})" } ?: "") + "."
            }
            ProposalKind.SKILLS -> "Task: suggest skills that are not in the SKILLS line yet."
            ProposalKind.ADVICE -> "Task: fix these problems:\n$problems"
        }
        val draft = if (current.isNotBlank()) "\n\nCURRENT DRAFT:\n$current" else ""
        val tweak = req.tweak?.let { "\n\nRewrite instruction: ${it.instruction}" }.orEmpty()
        return "$ctx\n\n$task$draft$tweak"
    }

    fun chatSystem(tab: String, r: Resume): String =
        "You are the AI helper inside a resume-builder app. The person is working on the \"$tab\" section, so answer about that section " +
            "unless they ask about something else. Jobs are numbered (JOB 1, JOB 2) in the resume; use those numbers when they say " +
            "\"my second job\". Use ONLY facts from the resume; never invent numbers, employers or skills; if a number is missing, " +
            "ask for it or use a [bracket] placeholder. Keep answers under 150 words, plain text, no markdown. " +
            "When you rewrite something, give the rewritten text first.\n\n" + AiContext.block(tab, r)
}

/** Safety check before AI text goes into the resume: numbers the person never wrote, and leftover [placeholders]. */
object AiGuard {
    private val numRe = Regex("\\d[\\d,.]*%?")
    private val placeholderRe = Regex("\\[[^\\]]{1,40}\\]")

    fun hasPlaceholders(text: String) = placeholderRe.containsMatchIn(text)

    /** Numbers in [proposed] that do not appear anywhere in the resume as it is now. */
    fun newNumbers(proposed: String, resume: Resume): List<String> {
        val known = numRe.findAll(AiContext.resumeText(resume)).map { it.value.trimEnd('.', ',') }.toSet()
        return numRe.findAll(proposed).map { it.value.trimEnd('.', ',') }
            .filter { it.isNotBlank() && it !in known }.distinct().toList()
    }
}

data class Check(val done: Boolean, val label: String, val tab: Int)

sealed interface SuggestionAction {
    data class Ai(val req: AiRequest) : SuggestionAction
    data class Tip(val text: String) : SuggestionAction
    data class GoTab(val tab: Int) : SuggestionAction
    data class SetTitle(val title: String) : SuggestionAction
    data class UseTemplate(val name: String, val why: String) : SuggestionAction
    data object EditTarget : SuggestionAction
}

data class Suggestion(val label: String, val action: SuggestionAction) {
    val usesAi: Boolean get() = action is SuggestionAction.Ai
}

/** Offline coaching: free, instant, and the base of every suggestion card. AI runs only when a card is tapped. */
object ResumeCoach {

    fun checks(r: Resume): List<Check> = listOf(
        Check(r.fullName.isNotBlank() && r.jobTitle.isNotBlank(), "Add your name and job title", 0),
        Check(r.email.isNotBlank() && r.phone.isNotBlank(), "Add email and phone", 0),
        Check(r.summary.length >= 60, "Write a summary of at least two sentences", 1),
        Check(r.experience.isNotEmpty(), "Add your work experience", 2),
        Check(r.experience.all { it.description.length >= 40 }, "Describe achievements for every job", 2),
        Check(r.education.isNotEmpty(), "Add your education", 3),
        Check(r.skills.size >= 5, "List at least 5 skills", 4),
        Check(r.location.isNotBlank(), "Add your location", 0),
    )

    fun strength(r: Resume): Int {
        val c = checks(r)
        return c.count { it.done } * 100 / c.size
    }

    fun nextStep(r: Resume): Check? = checks(r).firstOrNull { !it.done }

    fun yearsOfExperience(r: Resume): Int? {
        if (r.experience.isEmpty()) return null
        val years = r.experience.mapNotNull { Regex("(19|20)\\d{2}").find(it.startDate)?.value?.toIntOrNull() }
        val first = years.minOrNull() ?: return null
        val now = Calendar.getInstance().get(Calendar.YEAR)
        return (now - first).coerceAtLeast(0)
    }

    private fun bullets(desc: String) =
        desc.split("\n").map { it.trim().trimStart('•', '-', '*', '–').trim() }.filter { it.isNotBlank() }

    private val genericTitles = setOf(
        "developer", "engineer", "manager", "student", "fresher", "worker", "employee", "professional",
        "designer", "analyst", "intern", "executive", "consultant", "assistant", "officer",
    )

    fun suggestions(tab: Int, r: Resume, aiOn: Boolean): List<Suggestion> {
        val out = mutableListOf<Suggestion>()
        val target = TargetJob.parse(r)
        fun ai(label: String, req: AiRequest) { if (aiOn) out += Suggestion(label, SuggestionAction.Ai(req)) }

        when (tab) {
            0 -> {
                if (r.jobTitle.isBlank()) out += Suggestion("Add your job title", SuggestionAction.Tip("Type the job you want in the Job title field."))
                else if (r.jobTitle.trim().split(" ").size == 1 && r.jobTitle.trim().lowercase(Locale.US) in genericTitles)
                    out += Suggestion("Your job title is too general. Add a speciality", SuggestionAction.Tip("For example: \"Backend Developer\" instead of \"Developer\"."))
                if (target.title.isNotBlank() && r.jobTitle.isNotBlank() &&
                    target.title.lowercase(Locale.US) != r.jobTitle.lowercase(Locale.US) &&
                    target.title.lowercase(Locale.US).split(" ").none { it.length > 3 && r.jobTitle.lowercase(Locale.US).contains(it) }
                ) out += Suggestion("Use \"${target.title}\" as your title", SuggestionAction.SetTitle(target.title))
                if (r.linkedinUrl.isBlank()) out += Suggestion("Add your LinkedIn link", SuggestionAction.Tip("Paste your LinkedIn profile link in the LinkedIn field."))
                if (!target.isSet) out += Suggestion("Set the job you are applying for", SuggestionAction.EditTarget)
            }
            1 -> {
                if (r.summary.isBlank()) ai("Write my summary", AiRequest(ProposalKind.SUMMARY))
                else {
                    if (r.summary.length > 450) ai("Make the summary shorter", AiRequest(ProposalKind.SUMMARY, tweak = Tweak.SHORTER))
                    if (yearsOfExperience(r) != null && !Regex("\\d+\\+?\\s*(years|yrs)", RegexOption.IGNORE_CASE).containsMatchIn(r.summary))
                        ai("Add your years of experience", AiRequest(ProposalKind.SUMMARY, tweak = Tweak.YEARS))
                    ai("Make it stronger", AiRequest(ProposalKind.SUMMARY, tweak = Tweak.STRONGER))
                }
                if (!target.isSet) out += Suggestion("Set a target job so the summary fits it", SuggestionAction.EditTarget)
            }
            2 -> {
                if (r.experience.isEmpty()) out += Suggestion("Add your latest job first", SuggestionAction.Tip("Tap \"Add a job\" below."))
                val noNumbers = r.experience.mapIndexed { i, e -> i to bullets(e.description).count { b -> b.none { it.isDigit() } } }
                val total = noNumbers.sumOf { it.second }
                val firstWeak = noNumbers.firstOrNull { it.second > 0 }
                if (firstWeak != null && total > 0)
                    ai("$total bullet${if (total == 1) " has" else "s have"} no numbers. Add results", AiRequest(ProposalKind.BULLETS, firstWeak.first, Tweak.NUMBERS))
                val empty = r.experience.indexOfFirst { it.description.length < 40 && it.role.isNotBlank() }
                if (empty >= 0) ai("Write bullets for job ${empty + 1}", AiRequest(ProposalKind.BULLETS, empty))
                if (r.experience.isNotEmpty() && out.isEmpty()) ai("Make job 1 sound stronger", AiRequest(ProposalKind.BULLETS, 0, Tweak.STRONGER))
            }
            3 -> if (r.education.isEmpty()) out += Suggestion("Add your education, even if you are still studying", SuggestionAction.Tip("Tap \"Add education\" below."))
            4 -> {
                if (r.skills.size < 6) ai("Add 6 skills that match your target job", AiRequest(ProposalKind.SKILLS))
                else if (target.description.isNotBlank()) {
                    val missing = AtsAnalyzerEngine.analyzeText(AtsAnalyzerEngine.resumeToPlainText(r), target.description, emptyList()).missingKeywords
                    if (missing.isNotEmpty()) ai("Job needs: ${missing.take(3).joinToString(", ")}. Find matching skills", AiRequest(ProposalKind.SKILLS))
                }
                if (!target.isSet) out += Suggestion("Set a target job to get matching skills", SuggestionAction.EditTarget)
            }
            5 -> if (r.projects.isEmpty() && r.experience.size < 2)
                out += Suggestion("Add one project. It helps when you have little experience", SuggestionAction.Tip("Tap \"Add a project\" below."))
            6 -> if (r.languages.isEmpty()) out += Suggestion("Add the languages you speak", SuggestionAction.Tip("Tap \"Add language\" below."))
            7 -> {
                val (name, why) = templateFor(target.title.ifBlank { r.jobTitle }, target.description)
                if (ResumeHtmlHelper.normalizeTemplate(r.templateId) != name) out += Suggestion("Use \"$name\" for this job", SuggestionAction.UseTemplate(name, why))
            }
            else -> {
                val top = WritingCheck.analyze(r).take(3)
                if (top.isNotEmpty()) ai("Fix the top ${top.size} problems", AiRequest(ProposalKind.ADVICE))
            }
        }
        if (out.isEmpty()) ResumeCoach.nextStep(r)?.takeIf { it.tab != tab }?.let { out += Suggestion("Next: ${it.label}", SuggestionAction.GoTab(it.tab)) }
        return out.take(3)
    }

    /** Template name and a one-line reason, chosen from the job. Corporate roles get the plain ATS-safe one. */
    fun templateFor(title: String, description: String): Pair<String, String> {
        val t = "$title ${description.take(300)}".lowercase(Locale.US)
        // each word must start at a word boundary, so "intern" does not match "international"
        fun has(vararg w: String) = w.any { Regex("\\b$it").containsMatchIn(t) }
        return when {
            has("professor", "researcher", "phd", "lecturer", "academic", "research scientist", "postdoc") ->
                "Academic CV" to "Education and research come first, as academic readers expect."
            has("director", "vice president", "vp\\b", "chief", "head of", "executive", "ceo\\b", "cfo\\b", "cto\\b") ->
                "Executive Premium" to "A calm serif look that suits senior roles."
            has("intern\\b", "interns\\b", "internship", "fresher", "graduate", "trainee", "student", "entry.level") ->
                "Compact Two-Column" to "Education first and skills on the side, good when experience is short."
            has("developer", "software", "programmer", "devops", "data scientist", "data engineer", "machine learning", "engineer") ->
                "Developer Resume" to "Shows tech skills clearly for engineering roles."
            has("designer", "creative", "marketing", "brand", "content", "social media", "video", "photograph", "artist") ->
                "Premium Creative" to "A colour header that suits creative and marketing roles."
            has("accountant", "finance", "bank", "audit", "legal", "lawyer", "insurance", "government", "hr\\b", "human resources",
                "operations", "admin", "analyst", "manager", "sales", "consultant", "coordinator") ->
                "Minimal ATS" to "Plain single column that applicant systems read most reliably."
            else -> "Modern Professional" to "A clean all-round layout."
        }
    }
}
