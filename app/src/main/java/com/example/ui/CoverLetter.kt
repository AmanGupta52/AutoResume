package com.example.ui

import com.example.data.Resume
import java.text.DateFormat
import java.util.Date

object CoverLetter {

    /** Fill-in-the-blanks draft used when AI is not configured. Uses only facts from the resume. */
    fun offlineDraft(r: Resume, role: String, company: String, manager: String): String {
        val greeting = if (manager.isBlank()) "Dear Hiring Manager," else "Dear ${manager.trim()},"
        val targetRole = role.ifBlank { r.jobTitle.ifBlank { "open" } }
        val at = if (company.isBlank()) "" else " at ${company.trim()}"
        val latest = r.experience.firstOrNull { it.role.isNotBlank() }
        val firstBullet = latest?.description?.split("\n")?.map { it.trim().trimStart('•', '-', '*', ' ') }?.firstOrNull { it.isNotBlank() }
        val skills = r.skills.map { it.name }.filter { it.isNotBlank() }.take(5)
        val intro = r.summary.split(".").firstOrNull { it.isNotBlank() }?.trim()

        val p1 = "I am writing to apply for the $targetRole position$at. ${if (intro != null) "$intro." else ""}".trim()
        val p2 = buildString {
            if (latest != null) {
                append("In my role as ${latest.role}")
                if (latest.company.isNotBlank()) append(" at ${latest.company}")
                append(", ")
                if (firstBullet != null) append(firstBullet.replaceFirstChar { it.lowercase() }.trimEnd('.') + ". ") else append("I built the experience this position requires. ")
            }
            if (skills.isNotEmpty()) append("My strengths include ${skills.joinToString(", ")}.")
        }.trim()
        val p3 = "I would welcome the chance to discuss how my background can help your team. Thank you for your time and consideration."
        return listOf(greeting, p1, p2, p3, "Sincerely,\n${r.fullName.ifBlank { "" }}").filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** A4 letter that matches the resume accent colour and font style. */
    fun html(r: Resume, body: String, company: String, dark: Boolean = false): String {
        val opts = DesignOptions.parse(r)
        val accent = when (r.colorSchemeId) { "Green" -> "#1B8A3C"; "Cyan" -> "#0E7C9B"; "Slate" -> "#4B5563"; "Dark" -> "#1F2937"; else -> "#1F5FBF" }
        val font = when (opts.font) {
            "Serif" -> "'Noto Serif', Georgia, serif"
            "Mono" -> "'Roboto Mono', monospace"
            else -> "Roboto, 'Noto Sans', Arial, sans-serif"
        }
        val mm = ResumeHtmlHelper.marginMmOf(r.margins)
        val bg = if (dark) "#111827" else "#ffffff"
        val fg = if (dark) "#e5e7eb" else "#1f2937"
        val contact = listOf(r.email, r.phone, r.location).filter { it.isNotBlank() }.joinToString("  |  ") { esc(it) }
        val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date())
        val paragraphs = body.split("\n\n").filter { it.isNotBlank() }.joinToString("") { "<p>${esc(it.trim()).replace("\n", "<br>")}</p>" }
        return """<!DOCTYPE html><html><head><meta charset="utf-8"><title>Cover letter</title><style>
@page { size: A4; margin: ${mm + 6}mm; }
* { box-sizing: border-box; }
body { margin: 0; font-family: $font; font-size: 12px; line-height: 1.55; color: $fg; background: $bg; -webkit-print-color-adjust: exact; }
.head { border-bottom: 3px solid $accent; padding-bottom: 10px; margin-bottom: 22px; }
h1 { margin: 0; font-size: 26px; color: $accent; }
.role { color: #6b7280; margin-top: 2px; }
.contact { margin-top: 6px; color: #6b7280; font-size: 11px; }
.date { margin-bottom: 4px; }
p { margin: 0 0 12px; }
</style></head><body>
<div class="head"><h1>${esc(r.fullName.ifBlank { "Your Name" })}</h1><div class="role">${esc(r.jobTitle)}</div><div class="contact">$contact</div></div>
<div class="date">$date</div>
${if (company.isBlank()) "" else "<div style=\"margin-bottom:16px\"><b>${esc(company)}</b></div>"}
$paragraphs
</body></html>"""
    }
}
