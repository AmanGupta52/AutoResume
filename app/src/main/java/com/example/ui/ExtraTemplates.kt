package com.example.ui

import android.content.Context
import com.example.data.Resume

/** Five extra templates that share one renderer. Input resume must already be HTML-escaped. */
object ExtraTemplates {
    val ids = setOf("Compact Two-Column", "Academic CV", "Classic Serif", "Bold Header", "Timeline")

    private val accents = mapOf("Blue" to "#0A84FF", "Green" to "#30D158", "Cyan" to "#5AC8FA", "Slate" to "#8E8E93", "Dark" to "#1F2937")

    private fun darken(hex: String, f: Float = 0.62f): String {
        val v = hex.removePrefix("#").toInt(16)
        val r = (((v shr 16) and 0xFF) * f).toInt()
        val g = (((v shr 8) and 0xFF) * f).toInt()
        val b = ((v and 0xFF) * f).toInt()
        return String.format("#%02X%02X%02X", r, g, b)
    }

    /** Default section order that suits the template when the user has not customised it. */
    private fun preferredOrder(tpl: String): List<String> = when (tpl) {
        "Compact Two-Column" -> listOf("summary", "education", "projects", "experience", "achievements", "skills", "certifications", "languages")
        "Academic CV" -> listOf("education", "achievements", "experience", "projects", "certifications", "skills", "languages", "summary")
        else -> DesignOptions.DEFAULT_ORDER
    }

    private fun lines(desc: String): List<String> =
        desc.split("\n").map { it.trim().trimStart('•', '-', '*', '–').trim() }.filter { it.isNotBlank() }

    private fun bullets(desc: String): String {
        val l = lines(desc)
        return if (l.isEmpty()) "" else "<ul>" + l.joinToString("") { "<li>$it</li>" } + "</ul>"
    }

    private fun range(s: String, e: String, current: Boolean): String =
        listOfNotNull(s.ifBlank { null }, if (current) "Present" else e.ifBlank { null }).joinToString(" – ")

    private fun cleanUrl(u: String) = u.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')

    private fun link(u: String, label: String? = null): String =
        if (u.isBlank()) "" else "<a href=\"${if (u.startsWith("http")) u else "https://$u"}\">${label ?: cleanUrl(u)}</a>"

    private fun sectionBody(key: String, r: Resume, chips: Boolean): String = when (key) {
        "summary" -> if (r.summary.isBlank()) "" else "<p>${r.summary.replace("\n", "<br>")}</p>"
        "experience" -> r.experience.filter { it.role.isNotBlank() || it.company.isNotBlank() }.joinToString("") { e ->
            "<div class=\"item\"><div class=\"when\">${range(e.startDate, e.endDate, e.isCurrent)}</div>" +
                "<div class=\"what\"><div class=\"ttl\">${e.role}</div><div class=\"sub\">${e.company}</div>${bullets(e.description)}</div></div>"
        }
        "education" -> r.education.filter { it.institution.isNotBlank() || it.degree.isNotBlank() }.joinToString("") { e ->
            "<div class=\"item\"><div class=\"when\">${range(e.startYear, e.endYear, false)}</div>" +
                "<div class=\"what\"><div class=\"ttl\">${e.degree}</div>" +
                "<div class=\"sub\">${listOf(e.institution, e.grade).filter { it.isNotBlank() }.joinToString(" · ")}</div>${bullets(e.achievements)}</div></div>"
        }
        "skills" -> {
            val names = r.skills.map { it.name }.filter { it.isNotBlank() }
            if (names.isEmpty()) "" else if (chips) names.joinToString("") { "<span class=\"chip\">$it</span>" } else "<p>${names.joinToString(", ")}</p>"
        }
        "projects" -> r.projects.filter { it.title.isNotBlank() }.joinToString("") { p ->
            val links = listOf(link(p.githubLink), link(p.liveLink)).filter { it.isNotBlank() }.joinToString(" · ")
            "<div class=\"item\"><div class=\"when\">$links</div><div class=\"what\"><div class=\"ttl\">${p.title}</div>" +
                (if (p.techStack.isBlank()) "" else "<div class=\"sub\">${p.techStack}</div>") +
                (if (p.description.isBlank()) "" else "<p class=\"tight\">${p.description}</p>") + "</div></div>"
        }
        "certifications" -> r.certifications.filter { it.name.isNotBlank() }.joinToString("") { c ->
            "<div class=\"line\"><b>${c.name}</b>${if (c.issuer.isBlank()) "" else " — ${c.issuer}"}${if (c.date.isBlank()) "" else " <span class=\"muted\">(${c.date})</span>"}</div>"
        }
        "languages" -> r.languages.filter { it.name.isNotBlank() }.joinToString("") { l ->
            "<div class=\"line\">${l.name} <span class=\"muted\">${l.proficiency}</span></div>"
        }
        "achievements" -> bullets(r.achievements)
        else -> ""
    }

    fun render(context: Context, r: Resume, tpl: String, opts: DesignOptions, dark: Boolean, marginMm: Int): String {
        val accent = accents[r.colorSchemeId] ?: accents.getValue("Dark")
        val accentText = if (dark) accent else darken(accent)
        val bg = if (dark) "#111827" else "#ffffff"
        val fg = if (dark) "#e5e7eb" else "#1f2937"
        val muted = if (dark) "#9ca3af" else "#4b5563"
        val rule = if (dark) "#374151" else "#d1d5db"
        val px = when (r.fontSize) { "Compact" -> "10.5"; "Large" -> "12.5"; else -> "11.5" }
        val serif = tpl == "Academic CV" || tpl == "Classic Serif"
        val font = if (serif) "'Noto Serif', Georgia, serif" else "Roboto, 'Noto Sans', Arial, sans-serif"
        val order = if (opts.order == DesignOptions.DEFAULT_ORDER) preferredOrder(tpl) else opts.order
        val name = r.fullName.ifBlank { "Your Name" }
        val contact = listOf(r.email, r.phone, r.location).filter { it.isNotBlank() }
        val links = listOf(link(r.linkedinUrl, "LinkedIn"), link(r.githubUrl, "GitHub"), link(r.portfolioUrl, "Website")).filter { it.isNotBlank() }
        val avatar = ResumeHtmlHelper.getAvatarBase64(context, r.avatarUri)
        val chips = tpl == "Bold Header" || tpl == "Compact Two-Column" || tpl == "Timeline"

        fun sec(key: String): String {
            val body = sectionBody(key, r, chips)
            if (body.isBlank()) return ""
            val title = if (tpl == "Academic CV" && key == "achievements") "Honors, publications and other" else DesignOptions.SECTION_TITLES.getValue(key)
            return "<section><h2>$title</h2>$body</section>"
        }

        val css = """
@page { size: A4; margin: ${marginMm}mm; }
* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body { font-family: $font; font-size: ${px}px; line-height: 1.4; color: $fg; background: $bg; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
h1 { margin: 0; font-size: 2.1em; line-height: 1.15; }
h2 { font-size: 1.02em; margin: 14px 0 6px; }
section { margin-bottom: 4px; }
p { margin: 0 0 6px; }
.tight { margin: 2px 0 0; }
.item { display: flex; flex-direction: row-reverse; justify-content: space-between; gap: 12px; margin: 0 0 9px; break-inside: avoid; page-break-inside: avoid; }
.what { flex: 1; min-width: 0; }
.when { white-space: nowrap; color: $muted; font-size: .92em; text-align: right; }
.ttl { font-weight: 700; }
.sub { color: $muted; }
.muted { color: $muted; }
.line { margin: 0 0 3px; }
ul { margin: 3px 0 0 1.1em; padding: 0; }
li { margin: 0 0 2px; }
a { color: inherit; text-decoration: none; }
.chip { display: inline-block; margin: 0 5px 5px 0; padding: 2px 9px; border-radius: 999px; background: ${accent}26; color: $accentText; font-size: .92em; }
h2, .ttl { break-after: avoid; page-break-after: avoid; }
"""

        val extraCss: String
        val body: String
        when (tpl) {
            "Compact Two-Column" -> {
                extraCss = """
.page { display: flex; gap: 18px; align-items: flex-start; }
.side { width: 31%; background: ${accent}1F; padding: 14px; border-radius: 8px; }
.main { flex: 1; min-width: 0; }
.side h2 { margin-top: 12px; }
h1 { color: $accentText; }
h2 { color: $accentText; text-transform: uppercase; letter-spacing: .06em; font-size: .92em; border-bottom: 1px solid $rule; padding-bottom: 3px; }
.avatar { width: 84px; height: 84px; border-radius: 50%; object-fit: cover; display: block; margin: 0 0 8px; }
.contact div { margin-bottom: 3px; word-break: break-word; }
.role { color: $muted; font-size: 1.1em; margin: 2px 0 10px; }
"""
                val sideKeys = listOf("skills", "languages", "certifications")
                val side = (if (avatar != null) "<img class=\"avatar\" src=\"$avatar\">" else "") +
                    "<div class=\"contact\">${(contact + links).joinToString("") { "<div>$it</div>" }}</div>" +
                    order.filter { it in sideKeys }.joinToString("") { sec(it) }
                val main = "<h1>$name</h1><div class=\"role\">${r.jobTitle}</div>" +
                    order.filter { it !in sideKeys }.joinToString("") { sec(it) }
                body = "<div class=\"page\"><div class=\"side\">$side</div><div class=\"main\">$main</div></div>"
            }
            "Academic CV" -> {
                extraCss = """
.head { text-align: center; margin-bottom: 8px; }
h1 { font-size: 1.9em; font-weight: 600; }
.role { color: $muted; font-style: italic; }
.meta { margin-top: 4px; color: $muted; }
.meta span + span::before { content: "  |  "; }
h2 { font-variant: small-caps; font-size: 1.15em; letter-spacing: .04em; border-bottom: 1px solid $fg; padding-bottom: 2px; }
"""
                body = "<div class=\"head\"><h1>$name</h1><div class=\"role\">${r.jobTitle}</div>" +
                    "<div class=\"meta\">${(contact + links).joinToString("") { "<span>$it</span>" }}</div></div>" +
                    order.joinToString("") { sec(it) }
            }
            "Classic Serif" -> {
                extraCss = """
.head { text-align: center; border-bottom: 2px solid $fg; padding-bottom: 8px; margin-bottom: 6px; }
h1 { text-transform: uppercase; letter-spacing: .14em; font-size: 1.8em; font-weight: 600; }
.role { margin-top: 2px; letter-spacing: .06em; color: $muted; }
.meta { margin-top: 6px; color: $muted; }
.meta span + span::before { content: "  •  "; }
h2 { text-transform: uppercase; letter-spacing: .1em; font-size: .95em; border-bottom: 1px solid $rule; padding-bottom: 2px; }
"""
                body = "<div class=\"head\"><h1>$name</h1><div class=\"role\">${r.jobTitle}</div>" +
                    "<div class=\"meta\">${(contact + links).joinToString("") { "<span>$it</span>" }}</div></div>" +
                    order.joinToString("") { sec(it) }
            }
            "Bold Header" -> {
                extraCss = """
.band { background: ${darken(accent, 0.55f)}; color: #fff; padding: 22px 24px; display: flex; justify-content: space-between; align-items: center; gap: 16px; margin: 0 0 12px; border-radius: 10px; }
.band h1 { color: #fff; }
.band .role { opacity: .9; font-size: 1.1em; margin-top: 2px; }
.band .meta { margin-top: 8px; font-size: .95em; opacity: .95; }
.band .meta span + span::before { content: "  |  "; }
.avatar { width: 78px; height: 78px; border-radius: 50%; object-fit: cover; border: 3px solid rgba(255,255,255,.7); }
h2 { color: $accentText; text-transform: uppercase; letter-spacing: .08em; font-size: .95em; }
"""
                body = "<div class=\"band\"><div><h1>$name</h1><div class=\"role\">${r.jobTitle}</div>" +
                    "<div class=\"meta\">${(contact + links).joinToString("") { "<span>$it</span>" }}</div></div>" +
                    (if (avatar != null) "<img class=\"avatar\" src=\"$avatar\">" else "") + "</div>" +
                    order.joinToString("") { sec(it) }
            }
            else -> { // Timeline
                extraCss = """
.item { flex-direction: row; }
.when { width: 104px; flex: none; text-align: left; padding-top: 1px; }
h1 { border-left: 5px solid $accent; padding-left: 10px; }
.role { padding-left: 15px; color: $muted; }
.meta { padding-left: 15px; color: $muted; margin: 4px 0 8px; }
.meta span + span::before { content: "  •  "; }
h2 { color: $accentText; border-bottom: 2px solid $accent; display: inline-block; padding-bottom: 2px; font-size: 1em; }
"""
                body = "<h1>$name</h1><div class=\"role\">${r.jobTitle}</div>" +
                    "<div class=\"meta\">${(contact + links).joinToString("") { "<span>$it</span>" }}</div>" +
                    order.joinToString("") { sec(it) }
            }
        }

        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>$name</title><style>$css$extraCss</style></head><body>$body</body></html>"
    }
}
