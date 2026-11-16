package com.example.ui

import com.example.data.Resume
import java.util.Locale

enum class IssueLevel { HIGH, MEDIUM, LOW }

data class WritingIssue(val section: String, val where: String, val problem: String, val fix: String, val level: IssueLevel)

/** Offline wording and spelling check for resume text. */
object WritingCheck {

    private val weak = listOf(
        "responsible for" to "Start with an action verb: Led, Managed, Built, Delivered.",
        "duties included" to "Describe results, not duties.",
        "worked on" to "Say what you did and what changed: Built, Improved, Launched.",
        "helped with" to "Say your actual contribution: Supported, Drafted, Ran.",
        "helped to" to "Say your actual contribution.",
        "assisted with" to "Be specific about what you owned.",
        "participated in" to "Say what you contributed.",
        "tasked with" to "Replace with the action you took.",
        "in charge of" to "Use Led, Owned or Managed.",
        "hard worker" to "Show it with a result instead of claiming it.",
        "team player" to "Show it with an example of teamwork.",
        "go-getter" to "Cliche. Replace with a measurable result.",
        "think outside the box" to "Cliche. Give an example of a creative solution.",
        "references available" to "Not needed. Remove it to save space.",
        "etc" to "Be specific or remove it.",
    )

    private val misspell = mapOf(
        "recieve" to "receive", "seperate" to "separate", "definately" to "definitely", "managment" to "management",
        "acheive" to "achieve", "experiance" to "experience", "responsability" to "responsibility", "occured" to "occurred",
        "accomodate" to "accommodate", "teh" to "the", "adress" to "address", "enviroment" to "environment",
        "sucessful" to "successful", "succesful" to "successful", "langauge" to "language", "knowlege" to "knowledge",
        "proffesional" to "professional", "profesional" to "professional", "tommorow" to "tomorrow", "untill" to "until",
        "developement" to "development", "analyse" to "analyze", "calender" to "calendar", "comunication" to "communication",
        "excellant" to "excellent", "independant" to "independent", "maintainance" to "maintenance", "refered" to "referred",
    )

    private val emailRe = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun bulletLines(desc: String) =
        desc.split("\n").map { it.trim().trimStart('•', '-', '*', '–').trim() }.filter { it.isNotBlank() }

    fun analyze(r: Resume): List<WritingIssue> {
        val out = mutableListOf<WritingIssue>()

        if (r.email.isNotBlank() && !emailRe.matches(r.email.trim()))
            out += WritingIssue("Basics", "Email", "The email address looks invalid.", "Check for typos, for example name@example.com.", IssueLevel.HIGH)
        if (r.email.isBlank()) out += WritingIssue("Basics", "Email", "No email address.", "Recruiters need a way to reach you.", IssueLevel.HIGH)
        if (r.phone.isBlank()) out += WritingIssue("Basics", "Phone", "No phone number.", "Add a number where you can be reached.", IssueLevel.MEDIUM)

        // summary
        val s = r.summary.trim()
        if (s.isNotEmpty()) {
            if (s.length < 60) out += WritingIssue("Summary", "Summary", "The summary is very short.", "Aim for 2-4 sentences (200-400 characters).", IssueLevel.MEDIUM)
            if (s.length > 600) out += WritingIssue("Summary", "Summary", "The summary is long.", "Trim it to 4 sentences or fewer.", IssueLevel.LOW)
            checkText("Summary", "Summary", s, out)
        } else out += WritingIssue("Summary", "Summary", "No summary.", "A 2-3 sentence summary helps both recruiters and ATS.", IssueLevel.MEDIUM)

        // experience
        r.experience.forEachIndexed { i, e ->
            val where = "Job ${i + 1}" + if (e.role.isNotBlank()) " (${e.role})" else ""
            val bl = bulletLines(e.description)
            if (bl.isEmpty()) {
                out += WritingIssue("Experience", where, "No achievements listed.", "Add 3-5 bullets that show results.", IssueLevel.HIGH)
            } else {
                val withNumbers = bl.count { b -> b.any { it.isDigit() } }
                if (withNumbers * 2 < bl.size)
                    out += WritingIssue("Experience", where, "Only $withNumbers of ${bl.size} bullets contain a number.", "Add figures: %, money, team size, time saved, users.", IssueLevel.MEDIUM)
                val firstWords = bl.map { it.split(" ").first().lowercase(Locale.US) }
                firstWords.groupingBy { it }.eachCount().filter { it.value >= 3 && it.key.length > 2 }.forEach { (w, n) ->
                    out += WritingIssue("Experience", where, "$n bullets start with \"$w\".", "Vary your opening verbs.", IssueLevel.LOW)
                }
                bl.forEachIndexed { bi, b ->
                    val w = "$where, bullet ${bi + 1}"
                    if (b.length > 220) out += WritingIssue("Experience", w, "This bullet is very long.", "Split it or cut it to one line or two.", IssueLevel.LOW)
                    if (b.length < 25) out += WritingIssue("Experience", w, "This bullet is very short.", "Say what you did and the result.", IssueLevel.LOW)
                    if (b.first().isLowerCase()) out += WritingIssue("Experience", w, "Starts with a lowercase letter.", "Capitalise the first word.", IssueLevel.LOW)
                    checkText("Experience", w, b, out)
                }
            }
        }
        if (r.experience.isEmpty()) out += WritingIssue("Experience", "Experience", "No work experience listed.", "Add jobs, internships, volunteering or projects.", IssueLevel.MEDIUM)

        // skills
        if (r.skills.size < 5) out += WritingIssue("Skills", "Skills", "Fewer than 5 skills.", "List 6-12 skills that match the jobs you want.", IssueLevel.MEDIUM)

        // projects
        r.projects.forEachIndexed { i, p -> if (p.description.isNotBlank()) checkText("Projects", "Project ${i + 1}", p.description, out) }

        return out.sortedBy { it.level.ordinal }
    }

    private fun checkText(section: String, where: String, text: String, out: MutableList<WritingIssue>) {
        val lower = text.lowercase(Locale.US)
        weak.forEach { (phrase, fix) ->
            if (Regex("(?<![a-z])${Regex.escape(phrase)}(?![a-z])").containsMatchIn(lower))
                out += WritingIssue(section, where, "Weak phrase: \"$phrase\".", fix, IssueLevel.MEDIUM)
        }
        misspell.forEach { (wrong, right) ->
            if (Regex("(?<![a-z])$wrong(?![a-z])").containsMatchIn(lower))
                out += WritingIssue(section, where, "Possible misspelling: \"$wrong\".", "Did you mean \"$right\"?", IssueLevel.HIGH)
        }
        if (Regex("^(i|my|me)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text.trim()))
            out += WritingIssue(section, where, "Starts with a first-person pronoun.", "Resumes drop \"I\" and \"my\". Start with a verb.", IssueLevel.MEDIUM)
        if (text.contains("  ")) out += WritingIssue(section, where, "Double spaces.", "Use single spaces.", IssueLevel.LOW)
    }
}
