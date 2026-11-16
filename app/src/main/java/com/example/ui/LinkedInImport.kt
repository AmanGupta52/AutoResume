package com.example.ui

import android.content.Context
import android.net.Uri
import com.example.data.*
import java.util.zip.ZipInputStream

/** Builds a Resume from LinkedIn's "Get a copy of your data" zip (Profile, Positions, Education, Skills...). */
object LinkedInImport {

    /** Small CSV parser that handles quotes, escaped quotes and line breaks inside quoted cells. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        val s = text.removePrefix("\uFEFF")
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { cell.append('"'); i++ } else inQuotes = false
                } else cell.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(cell.toString()); cell.clear() }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    private fun table(text: String?): List<Map<String, String>> {
        if (text == null) return emptyList()
        val rows = parseCsv(text)
        if (rows.size < 2) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1).map { r -> header.indices.associate { header[it] to r.getOrElse(it) { "" }.trim() } }
    }

    private fun bulletize(desc: String): String =
        desc.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            .joinToString("\n") { if (it.startsWith("•") || it.startsWith("-")) "• " + it.trimStart('•', '-', ' ') else "• $it" }

    fun fromZip(context: Context, uri: Uri): Resume? {
        val files = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { z ->
                    var e = z.nextEntry
                    while (e != null) {
                        val name = e.name.substringAfterLast('/').lowercase()
                        if (!e.isDirectory && name.endsWith(".csv") && name in setOf("profile.csv", "positions.csv", "education.csv", "skills.csv", "certifications.csv", "languages.csv")) {
                            files[name] = z.readBytes().toString(Charsets.UTF_8)
                        }
                        z.closeEntry()
                        e = z.nextEntry
                    }
                }
            }
        } catch (ex: Exception) {
            return null
        }
        if (files.isEmpty()) return null

        val profile = table(files["profile.csv"]).firstOrNull() ?: emptyMap()
        val name = listOf(profile["First Name"], profile["Last Name"]).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")

        val experience = table(files["positions.csv"]).map {
            ExperienceEntry(
                company = it["Company Name"].orEmpty(), role = it["Title"].orEmpty(),
                startDate = it["Started On"].orEmpty(), endDate = it["Finished On"].orEmpty(),
                isCurrent = it["Finished On"].isNullOrBlank(), description = bulletize(it["Description"].orEmpty()),
            )
        }
        val education = table(files["education.csv"]).map {
            EducationEntry(
                institution = it["School Name"].orEmpty(), degree = it["Degree Name"].orEmpty(),
                startYear = it["Start Date"].orEmpty().takeLast(4), endYear = it["End Date"].orEmpty().takeLast(4),
                grade = "", achievements = it["Notes"].orEmpty(),
            )
        }
        val skills = table(files["skills.csv"]).mapNotNull { it["Name"]?.takeIf { n -> n.isNotBlank() } }.map { SkillEntry(it, "Intermediate", "Technical") }
        val certs = table(files["certifications.csv"]).map {
            CertificationEntry(name = it["Name"].orEmpty(), issuer = it["Authority"].orEmpty(), date = it["Started On"].orEmpty(), url = it["Url"].orEmpty())
        }
        val langs = table(files["languages.csv"]).mapNotNull { row ->
            row["Name"]?.takeIf { it.isNotBlank() }?.let { LanguageEntry(it, row["Proficiency"]?.takeIf { p -> p.isNotBlank() } ?: "Fluent") }
        }

        return Resume(
            name = "Imported from LinkedIn",
            fullName = name, jobTitle = profile["Headline"].orEmpty(), summary = profile["Summary"].orEmpty(),
            location = profile["Geo Location"].orEmpty(),
            experience = experience, education = education, skills = skills, certifications = certs, languages = langs,
        )
    }
}
