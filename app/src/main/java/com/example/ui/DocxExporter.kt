package com.example.ui

import android.content.Context
import com.example.data.Resume
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a plain, ATS-friendly .docx with no external libraries (a .docx is a zip of XML). */
object DocxExporter {

    private fun x(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun run(text: String, bold: Boolean = false, size: Int = 21, color: String? = null, italic: Boolean = false): String =
        "<w:r><w:rPr><w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:cs=\"Calibri\"/>" +
            (if (bold) "<w:b/>" else "") + (if (italic) "<w:i/>" else "") +
            (if (color != null) "<w:color w:val=\"$color\"/>" else "") +
            "<w:sz w:val=\"$size\"/></w:rPr><w:t xml:space=\"preserve\">${x(text)}</w:t></w:r>"

    private fun para(
        runs: String,
        after: Int = 60,
        before: Int = 0,
        center: Boolean = false,
        rightTab: Boolean = false,
        border: String? = null,
        indent: Boolean = false,
    ): String {
        val ppr = StringBuilder("<w:pPr>")
        if (border != null) ppr.append("<w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"$border\"/></w:pBdr>")
        if (rightTab) ppr.append("<w:tabs><w:tab w:val=\"right\" w:pos=\"9638\"/></w:tabs>")
        ppr.append("<w:spacing w:before=\"$before\" w:after=\"$after\"/>")
        if (indent) ppr.append("<w:ind w:left=\"360\" w:hanging=\"240\"/>")
        if (center) ppr.append("<w:jc w:val=\"center\"/>")
        ppr.append("</w:pPr>")
        return "<w:p>$ppr$runs</w:p>"
    }

    private fun heading(t: String, color: String) = para(run(t.uppercase(), bold = true, size = 23, color = color), after = 80, before = 200, border = color)

    private fun bullets(desc: String): String =
        desc.split("\n").map { it.trim().trimStart('•', '-', '*', '–').trim() }.filter { it.isNotBlank() }
            .joinToString("") { para(run("•  $it"), after = 30, indent = true) }

    private fun range(s: String, e: String, cur: Boolean) =
        listOfNotNull(s.ifBlank { null }, if (cur) "Present" else e.ifBlank { null }).joinToString(" – ")

    fun export(context: Context, r: Resume): File {
        val opts = DesignOptions.parse(r)
        val accent = when (r.colorSchemeId) { "Green" -> "1B8A3C"; "Cyan" -> "0E7C9B"; "Slate" -> "4B5563"; "Dark" -> "1F2937"; else -> "1F5FBF" }
        val body = StringBuilder()

        body.append(para(run(r.fullName.ifBlank { "Your Name" }, bold = true, size = 44, color = accent), after = 20, center = true))
        if (r.jobTitle.isNotBlank()) body.append(para(run(r.jobTitle, size = 26, color = "4B5563"), after = 40, center = true))
        val contact = listOf(r.email, r.phone, r.location, r.linkedinUrl, r.githubUrl, r.portfolioUrl).filter { it.isNotBlank() }
        if (contact.isNotEmpty()) body.append(para(run(contact.joinToString("  |  "), size = 19, color = "4B5563"), after = 80, center = true))

        opts.order.forEach { key ->
            when (key) {
                "summary" -> if (r.summary.isNotBlank()) { body.append(heading("Summary", accent)); body.append(para(run(r.summary))) }
                "experience" -> if (r.experience.any { it.role.isNotBlank() || it.company.isNotBlank() }) {
                    body.append(heading("Experience", accent))
                    r.experience.filter { it.role.isNotBlank() || it.company.isNotBlank() }.forEach { e ->
                        body.append(para(run(e.role, bold = true) + "<w:r><w:tab/></w:r>" + run(range(e.startDate, e.endDate, e.isCurrent), size = 19, color = "4B5563"), after = 10, before = 60, rightTab = true))
                        if (e.company.isNotBlank()) body.append(para(run(e.company, italic = true, color = "4B5563"), after = 30))
                        body.append(bullets(e.description))
                    }
                }
                "education" -> if (r.education.any { it.institution.isNotBlank() || it.degree.isNotBlank() }) {
                    body.append(heading("Education", accent))
                    r.education.filter { it.institution.isNotBlank() || it.degree.isNotBlank() }.forEach { e ->
                        body.append(para(run(e.degree, bold = true) + "<w:r><w:tab/></w:r>" + run(range(e.startYear, e.endYear, false), size = 19, color = "4B5563"), after = 10, before = 60, rightTab = true))
                        val sub = listOf(e.institution, e.grade).filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotBlank()) body.append(para(run(sub, italic = true, color = "4B5563"), after = 30))
                        body.append(bullets(e.achievements))
                    }
                }
                "skills" -> if (r.skills.any { it.name.isNotBlank() }) {
                    body.append(heading("Skills", accent))
                    body.append(para(run(r.skills.map { it.name }.filter { it.isNotBlank() }.joinToString(", "))))
                }
                "projects" -> if (r.projects.any { it.title.isNotBlank() }) {
                    body.append(heading("Projects", accent))
                    r.projects.filter { it.title.isNotBlank() }.forEach { p ->
                        body.append(para(run(p.title, bold = true) + (if (p.techStack.isBlank()) "" else run("  –  ${p.techStack}", color = "4B5563")), after = 20, before = 60))
                        if (p.description.isNotBlank()) body.append(para(run(p.description), after = 30))
                        val links = listOf(p.githubLink, p.liveLink).filter { it.isNotBlank() }
                        if (links.isNotEmpty()) body.append(para(run(links.joinToString("  |  "), size = 19, color = "4B5563"), after = 30))
                    }
                }
                "certifications" -> if (r.certifications.any { it.name.isNotBlank() }) {
                    body.append(heading("Certifications", accent))
                    r.certifications.filter { it.name.isNotBlank() }.forEach { c ->
                        body.append(para(run(c.name, bold = true) + run(listOf(c.issuer, c.date).filter { it.isNotBlank() }.joinToString(", ", prefix = "  –  ")), after = 30))
                    }
                }
                "languages" -> if (r.languages.any { it.name.isNotBlank() }) {
                    body.append(heading("Languages", accent))
                    body.append(para(run(r.languages.filter { it.name.isNotBlank() }.joinToString("  ·  ") { "${it.name} (${it.proficiency})" })))
                }
                "achievements" -> if (r.achievements.isNotBlank()) {
                    body.append(heading("Awards and interests", accent))
                    body.append(bullets(r.achievements))
                }
            }
        }

        val document = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>$body" +
            "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/></w:sectPr></w:body></w:document>"
        val contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>"
        val rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>"

        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val safe = r.fullName.ifBlank { "Resume" }.replace(Regex("[^A-Za-z0-9_-]+"), "_")
        val file = File(dir, "${safe}_Resume.docx")
        ZipOutputStream(file.outputStream()).use { z ->
            fun put(name: String, text: String) { z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rels)
            put("word/document.xml", document)
        }
        return file
    }
}
