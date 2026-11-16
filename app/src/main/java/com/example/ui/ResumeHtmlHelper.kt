package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ResumeHtmlHelper {

    fun printResume(context: Context, resume: Resume, isDarkExport: Boolean) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            try {
                val webView = android.webkit.WebView(context)
                val htmlContent = generateHtml(context, resume, isDarkExport)
                
                webView.settings.allowFileAccess = false
                webView.settings.allowContentAccess = false
                webView.settings.domStorageEnabled = true
                
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        try {
                            val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                            val jobName = "${resume.fullName.replace(" ", "_")}_Resume"
                            val printAdapter = webView.createPrintDocumentAdapter(jobName)
                            
                            val printAttributes = android.print.PrintAttributes.Builder()
                                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                                .build()
                            
                            printManager.print(jobName, printAdapter, printAttributes)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Cannot initiate printing sheet: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "utf-8", null)
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error starting print job: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getAvatarBase64(context: Context, avatarUri: String): String? {
        if (avatarUri.isBlank()) return null
        if (avatarUri == "initials") return null
        try {
            val uri = android.net.Uri.parse(avatarUri)
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
                // Resize if too huge
                val maxDim = 300
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                    val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                    Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                } else {
                    bitmap
                }
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun marginMmOf(margins: String): Int = when (margins) { "Narrow" -> 8; "Wide" -> 18; else -> 12 }

    private fun String.h() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Every user-typed string is escaped once here, so "&" or "<" can never break the document. */
    private fun escapeResume(r: Resume): Resume = r.copy(
        name = r.name.h(), fullName = r.fullName.h(), jobTitle = r.jobTitle.h(), phone = r.phone.h(), email = r.email.h(),
        location = r.location.h(), linkedinUrl = r.linkedinUrl.h(), githubUrl = r.githubUrl.h(), portfolioUrl = r.portfolioUrl.h(),
        summary = r.summary.h(), achievements = r.achievements.h(),
        education = r.education.map { it.copy(institution = it.institution.h(), degree = it.degree.h(), startYear = it.startYear.h(), endYear = it.endYear.h(), grade = it.grade.h(), achievements = it.achievements.h()) },
        experience = r.experience.map { it.copy(company = it.company.h(), role = it.role.h(), startDate = it.startDate.h(), endDate = it.endDate.h(), description = it.description.h()) },
        skills = r.skills.map { it.copy(name = it.name.h(), level = it.level.h(), category = it.category.h()) },
        projects = r.projects.map { it.copy(title = it.title.h(), techStack = it.techStack.h(), description = it.description.h(), githubLink = it.githubLink.h(), liveLink = it.liveLink.h()) },
        certifications = r.certifications.map { it.copy(name = it.name.h(), issuer = it.issuer.h(), date = it.date.h(), credentialId = it.credentialId.h(), url = it.url.h()) },
        languages = r.languages.map { it.copy(name = it.name.h(), proficiency = it.proficiency.h()) },
    )

    private fun fontStack(font: String): String? = when (font) {
        "Sans" -> "Roboto, 'Noto Sans', Arial, sans-serif"
        "Serif" -> "'Noto Serif', Georgia, serif"
        "Mono" -> "'Roboto Mono', monospace"
        else -> null
    }

    /**
     * The ONE place every output (preview, thumbnail, print, PDF) comes from.
     * [thumbnail] gives a clean first-page miniature for the resume list.
     */
    fun generateHtml(
        context: Context,
        resume: Resume,
        isDarkExport: Boolean = false,
        forPreview: Boolean = false,
        thumbnail: Boolean = false,
    ): String {
        val opts = DesignOptions.parse(resume.fontSize)
        val safe = escapeResume(resume).copy(fontSize = opts.size)
        val tpl = normalizeTemplate(resume.templateId)
        val mm = marginMmOf(resume.margins)
        var html = if (tpl in ExtraTemplates.ids) ExtraTemplates.render(context, safe, tpl, opts, isDarkExport, mm)
        else generateLegacy(context, safe, isDarkExport, false)

        val extra = StringBuilder()
        fontStack(opts.font)?.let { extra.append("<style>body, body * { font-family: $it !important; }</style>") }
        if (opts.tight) extra.append("<style>body { line-height: 1.25 !important; } p, li { margin-top: 0 !important; margin-bottom: 2px !important; } h2, h3 { margin-top: 8px !important; margin-bottom: 4px !important; }</style>")
        if (forPreview) extra.append(
            "<meta name=\"viewport\" content=\"width=794, user-scalable=yes\">" +
                "<style>html { background: ${if (isDarkExport) "#0b0f17" else "#cfd6e0"}; } " +
                "body { box-sizing: border-box; width: 794px; margin: 18px auto; padding: ${mm}mm; min-height: 1123px; box-shadow: 0 8px 28px rgba(0,0,0,.28); }</style>"
        )
        if (thumbnail) extra.append(
            "<meta name=\"viewport\" content=\"width=794\">" +
                "<style>html, body { margin: 0; overflow: hidden; background: #fff; } " +
                "body { box-sizing: border-box; width: 794px; padding: ${mm}mm; }</style>"
        )
        return html.replace("</head>", extra.toString() + "</head>")
    }

    private fun generateLegacy(
        context: Context,
        resume: Resume,
        isDarkExport: Boolean = false,
        forPreview: Boolean = false,
    ): String {
        val templateName = normalizeTemplate(resume.templateId)
        // Font size / margin settings chosen in the editor now actually reach the document.
        val baseFontPx = when (resume.fontSize) { "Compact" -> "10.5"; "Large" -> "12.5"; else -> "11.5" }
        val marginMm = when (resume.margins) { "Narrow" -> 8; "Wide" -> 18; else -> 12 }
        val base64Avatar = getAvatarBase64(context, resume.avatarUri)
        
        // Pick primary theme colors
        val primaryColor = when (resume.colorSchemeId) {
            "Blue" -> "#0A84FF"
            "Green" -> "#30D158"
            "Cyan" -> "#5AC8FA"
            "Slate" -> "#8E8E93"
            else -> "#1F2937" // Dark / corporate
        }

        // Executive luxury accent mapping (e.g. gold accent for executive)
        val accentColor = if (templateName == "Executive Premium") "#D4AF37" else primaryColor

        // Core visual colors
        val bgColor = if (isDarkExport) "#111827" else "#ffffff"
        val textMain = if (isDarkExport) "#f3f4f6" else "#111827"
        val textSec = if (isDarkExport) "#9ca3af" else "#4b5563"
        val borderLine = if (isDarkExport) "#374151" else "#e5e7eb"
        val cardBg = if (isDarkExport) "#1f2937" else "#f9fafb"

        // Setup font families based on template
        val fontFamily = when (templateName) {
            "Minimal ATS" -> "Roboto, 'Noto Sans', Arial, sans-serif"
            "Developer Resume" -> "'Roboto Mono', 'Droid Sans Mono', monospace"
            "Executive Premium" -> "'Noto Serif', Georgia, serif"
            else -> "Roboto, 'Noto Sans', sans-serif"
        }

        val nameInitials = resume.fullName.split(" ")
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .take(2)
            .uppercase()

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Resume Export</title>
    <style>
        @page {
            size: A4;
            margin: ${marginMm}mm;
        }
        * {
            box-sizing: border-box;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }
        body {
            font-family: $fontFamily;
            background-color: $bgColor;
            color: $textMain;
            margin: 0;
            padding: 0;
            font-size: ${baseFontPx}px;
            line-height: 1.45;
        }
        h1, h2, h3, h4 {
            margin: 0;
            padding: 0;
            font-weight: 700;
        }
        p {
            margin: 0 0 8px 0;
        }
        a {
            color: $primaryColor;
            text-decoration: none;
        }
        
        /* Layout Configurations */
        
        /* Thin Dividers */
        .divider {
            height: 1px;
            background-color: $borderLine;
            margin: 10px 0;
            border: none;
        }
        
        /* Icon contacts layout */
        .contacts-bar {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            margin-top: 10px;
            font-size: 10.5px;
            color: $textSec;
        }
        .contact-item {
            display: flex;
            align-items: center;
            gap: 4px;
        }
        
        /* Skill tags / progress bars style */
        .skills-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .tech-tag {
            background-color: $cardBg;
            border: 1px solid $borderLine;
            color: $textMain;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 10px;
            font-weight: 500;
        }
        
        .progress-bar-container {
            width: 100%;
            background-color: ${if (isDarkExport) "#374151" else "#e5e7eb"};
            height: 5px;
            border-radius: 3px;
            margin-top: 3px;
            margin-bottom: 8px;
            overflow: hidden;
        }
        .progress-bar-fill {
            background-color: $primaryColor;
            height: 100%;
            border-radius: 3px;
        }
        
        /* Section styling */
        .section-title {
            color: $primaryColor;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.8px;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            font-weight: bold;
        }
        
        /* Multi-page optimization: keep elements together */
        .avoid-break {
            page-break-inside: avoid;
            break-inside: avoid;
        }
        
        /* Timeline styling for Creative */
        .timeline-item {
            position: relative;
            padding-left: 14px;
            border-left: 2px solid $borderLine;
            margin-bottom: 12px;
        }
        .timeline-item::before {
            content: '';
            position: absolute;
            left: -5px;
            top: 4px;
            width: 8px;
            height: 8px;
            background-color: $primaryColor;
            border-radius: 50%;
        }

        /* ------------------ TEMPLATE SPECIFIC STYLES ------------------ */

        /* 1. Modern Professional */
        .template-modern-prof {
            display: grid;
            grid-template-columns: 210px 1fr;
            gap: 20px;
        }
        .modern-prof-sidebar {
            border-right: 1px solid $borderLine;
            padding-right: 15px;
        }
        
        /* 2. Minimal ATS */
        .template-minimal-ats {
            max-width: 800px;
            margin: 0 auto;
        }
        .minimal-ats-header {
            text-align: center;
            border-bottom: 2px solid #111827;
            padding-bottom: 10px;
            margin-bottom: 15px;
        }
        .minimal-ats-title {
            font-size: 22px;
            text-transform: uppercase;
            font-family: 'Times New Roman', Times, serif;
            letter-spacing: 1.5px;
            color: #111827;
        }
        .minimal-ats-headers {
            border-bottom: 1px solid #111827;
            padding-bottom: 2px;
            margin-top: 15px;
            margin-bottom: 8px;
            color: #111827;
            font-size: 12.5px;
            font-weight: bold;
            text-transform: uppercase;
        }
        
        /* 3. Premium Creative */
        .template-creative {
            display: grid;
            grid-template-columns: 220px 1fr;
            gap: 24px;
        }
        .creative-sidebar {
            background-color: $cardBg;
            padding: 15px;
            border-radius: 12px;
            border: 1px solid $borderLine;
        }
        .creative-header-card {
            background: linear-gradient(135deg, $primaryColor, ${if (primaryColor == "#1F2937") "#4b5563" else primaryColor + "cc"});
            color: #ffffff;
            padding: 18px;
            border-radius: 12px;
            margin-bottom: 16px;
        }
        
        /* 4. Developer Resume */
        .template-developer {
            max-width: 800px;
            margin: 0 auto;
        }
        .developer-header {
            background-color: #111827;
            color: #00ff66; /* default terminal tech color, or fallback */
            padding: 20px;
            border-radius: 8px;
            font-family: monospace;
            margin-bottom: 16px;
            border-left: 5px solid $primaryColor;
        }
        .monospace-accent {
            font-family: monospace;
            background-color: $cardBg;
            padding: 1px 4px;
            border-radius: 2px;
            font-size: 9.5px;
            border: 1px solid $borderLine;
        }
        
        /* 5. Executive Premium */
        .template-executive {
            border-top: 8px solid $primaryColor;
            padding-top: 15px;
        }
        .executive-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
        }
        .executive-name-box {
            border-left: 4px solid $accentColor;
            padding-left: 12px;
        }
        
        /* Profile Image Layouts */
        .profile-img-circle {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            object-fit: cover;
            border: 2px solid $primaryColor;
        }
        .profile-img-stylish {
            width: 100%;
            height: auto;
            max-height: 180px;
            border-radius: 8px;
            object-fit: cover;
            margin-bottom: 12px;
            border: 2px solid $primaryColor;
        }
        .monogram-avatar {
            width: 80px;
            height: 80px;
            border-radius: 50%;
            background-color: $primaryColor;
            color: #ffffff;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 32px;
            font-weight: bold;
            border: 2px solid $primaryColor;
        }
    </style>
</head>
<body>
""")

        // RENDER CONTAINER ACCORDING TO SELECTED TEMPLATENAME

        when (templateName) {
            "Minimal ATS" -> {
                // Template 2 Details: Minimal B&W, Single-Column, Clean
                htmlBuilder.append("""
                <div class="template-minimal-ats">
                    <div class="minimal-ats-header">
                        <h1 class="minimal-ats-title" style="color: #111827;">${resume.fullName}</h1>
                        <div style="font-size: 10.5px; color: #4b5563; margin-top: 6.5px;">
                            ${if (resume.email.isNotBlank()) "${resume.email} &nbsp;|&nbsp; " else ""}
                            ${if (resume.phone.isNotBlank()) "${resume.phone} &nbsp;|&nbsp; " else ""}
                            ${if (resume.location.isNotBlank()) "${resume.location} &nbsp;|&nbsp; " else ""}
                            ${if (resume.linkedinUrl.isNotBlank()) "LinkedIn: ${resume.linkedinUrl}" else ""}
                        </div>
                        <div style="font-size: 10.5px; color: #4b5563; margin-top: 4px;">
                            ${if (resume.githubUrl.isNotBlank()) "GitHub: ${resume.githubUrl} &nbsp;|&nbsp; " else ""}
                            ${if (resume.portfolioUrl.isNotBlank()) "Portfolio: ${resume.portfolioUrl}" else ""}
                        </div>
                    </div>
                """)

                if (resume.summary.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="minimal-ats-headers">Career Objective</div>
                        <p style="text-align: justify; color: #111827;">${resume.summary}</p>
                    </div>
                    """)
                }

                if (resume.skills.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="minimal-ats-headers">Skills</div>
                        <div class="skills-grid">
                    """)
                    for (skill in resume.skills) {
                        htmlBuilder.append("<span class=\"tech-tag\" style=\"border: none; background: #eaeaea; font-family: sans-serif; color: #000;\">${skill.name} (${skill.level})</span>")
                    }
                    htmlBuilder.append("""
                        </div>
                    </div>
                    """)
                }

                if (resume.experience.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div>
                        <div class="minimal-ats-headers">Employment & Internships</div>
                    """)
                    for (exp in resume.experience) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 12px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; text-align: left; color:#111827; font-size:11.5px;">${exp.role} — ${exp.company}</td>
                                    <td style="text-align: right; color:#4b5563; font-size:11px;">${exp.startDate} - ${exp.endDate}</td>
                                </tr>
                            </table>
                            <div style="margin-top: 4px; padding-left: 10px; color: #374151;">
                        """)
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        for (bullet in bullets) {
                            val cleanBullet = if (bullet.trim().startsWith("•")) bullet.trim() else "• ${bullet.trim()}"
                            htmlBuilder.append("<p style=\"margin-bottom: 3px;\">$cleanBullet</p>")
                        }
                        htmlBuilder.append("""
                            </div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.projects.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div>
                        <div class="minimal-ats-headers">Projects</div>
                    """)
                    for (proj in resume.projects) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 10px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; text-align: left; color:#111827; font-size:11px;">${proj.title} ${if (proj.techStack.isNotBlank()) "(${proj.techStack})" else ""}</td>
                                    <td style="text-align: right; font-size:10.5px;">
                                        ${if (proj.githubLink.isNotBlank()) "<a href=\"${proj.githubLink}\" style=\"color:#111827; text-decoration:underline; margin-right:8px;\">Repo</a>" else ""}
                                        ${if (proj.liveLink.isNotBlank()) "<a href=\"${proj.liveLink}\" style=\"color:#111827; text-decoration:underline;\">Live</a>" else ""}
                                    </td>
                                </tr>
                            </table>
                            <p style="margin-top: 3px; padding-left: 10px; color: #374151; font-size:11px;">${proj.description}</p>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.education.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div>
                        <div class="minimal-ats-headers">Education</div>
                    """)
                    for (edu in resume.education) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 8px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; text-align: left; color:#111827;">${edu.degree}</td>
                                    <td style="text-align: right; color:#4b5563; font-size:11px;">${edu.startYear} - ${edu.endYear}</td>
                                </tr>
                                <tr>
                                    <td style="color:#4b5563; font-size:11px;">${edu.institution}</td>
                                    <td style="text-align: right; color:#4b5563; font-size:11px;">${if (edu.grade.isNotBlank()) "GPA/Grade: ${edu.grade}" else ""}</td>
                                </tr>
                            </table>
                            ${if (edu.achievements.isNotBlank()) "<div style=\"font-size:10.5px; color:#4b5563; padding-left: 10px; margin-top:2px;\">Achievements: ${edu.achievements}</div>" else ""}
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.certifications.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="minimal-ats-headers">Certifications</div>
                    """)
                    for (cert in resume.certifications) {
                        htmlBuilder.append("<p style=\"margin-bottom: 4px; color:#111827;\"><b>${cert.name}</b> — ${cert.issuer} (${cert.date})</p>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.achievements.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="minimal-ats-headers">Achievements & Extracurriculars</div>
                        <p style="color:#111827;">${resume.achievements}</p>
                    </div>
                    """)
                }

                htmlBuilder.append("</div>") // End minimal ats container
            }

            "Premium Creative" -> {
                // Template 3 Details: Stylish sidebar (TwoColumn), skill progress indicators, timeline experience
                htmlBuilder.append("""
                <div class="template-creative">
                    <div class="creative-sidebar">
                        <div class="avoid-break" style="text-align: center; margin-bottom: 15px;">
                """)

                if (base64Avatar != null) {
                    htmlBuilder.append("<img src=\"$base64Avatar\" class=\"profile-img-stylish\" />")
                } else {
                    htmlBuilder.append("<div class=\"monogram-avatar\" style=\"margin: 0 auto 12px auto;\">$nameInitials</div>")
                }

                htmlBuilder.append("""
                            <h2 style="font-size: 14px; margin-top: 6px; color: $primaryColor;">${resume.jobTitle}</h2>
                        </div>
                        
                        <div class="avoid-break">
                            <div class="section-title">Contact Info</div>
                            <p style="font-size:10.5px; color:$textSec;">
                                ${if (resume.email.isNotBlank()) "${resume.email}<br>" else ""}
                                ${if (resume.phone.isNotBlank()) "${resume.phone}<br>" else ""}
                                ${if (resume.location.isNotBlank()) "${resume.location}<br>" else ""}
                                ${if (resume.linkedinUrl.isNotBlank()) "<a href=\"${resume.linkedinUrl}\">LinkedIn</a><br>" else ""}
                                ${if (resume.githubUrl.isNotBlank()) "<a href=\"${resume.githubUrl}\">GitHub</a>" else ""}
                            </p>
                        </div>
                        <div class="divider"></div>
                """)

                if (resume.skills.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Skills Map</div>
                    """)
                    for (skill in resume.skills) {
                        val pct = when (skill.level.lowercase()) {
                            "beginner" -> "33%"
                            "intermediate" -> "66%"
                            "expert", "advanced", "native" -> "100%"
                            else -> "75%"
                        }
                        htmlBuilder.append("""
                        <div style="font-size: 10.5px; margin-bottom: 4px; color: $textMain;">
                            <div style="display: flex; justify-content: space-between;">
                                <span>${skill.name}</span>
                                <span style="font-size:9px; color:$textSec">${skill.level}</span>
                            </div>
                            <div class="progress-bar-container">
                                <div class="progress-bar-fill" style="width: $pct;"></div>
                            </div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div><div class=\"divider\"></div>")
                }

                if (resume.languages.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Languages</div>
                        <p style="font-size:10.5px; color:$textSec;">
                    """)
                    for (lang in resume.languages) {
                        htmlBuilder.append("${lang.name} - ${lang.proficiency}<br>")
                    }
                    htmlBuilder.append("</p></div>")
                }

                htmlBuilder.append("""
                    </div>
                    <div class="creative-main">
                        <div class="creative-header-card avoid-break">
                            <h1 style="font-size: 24px; font-weight: 800; text-transform: uppercase;">${resume.fullName}</h1>
                            <p style="margin: 4px 0 0 0; opacity: 0.9; font-size:12.5px;">${resume.jobTitle}</p>
                        </div>
                """)

                if (resume.summary.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title">About Me</div>
                        <p style="text-align: justify; color: $textMain;">${resume.summary}</p>
                    </div>
                    """)
                }

                if (resume.experience.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Experience Timeline</div>
                    """)
                    for (exp in resume.experience) {
                        htmlBuilder.append("""
                        <div class="timeline-item avoid-break">
                            <h3 style="font-size: 11.5px; color: $textMain;">${exp.role}</h3>
                            <div style="font-size: 10px; color: $primaryColor; font-weight: 600; margin-bottom:4px;">${exp.company} &nbsp;|&nbsp; ${exp.startDate} - ${exp.endDate}</div>
                            <div style="color: $textSec;">
                        """)
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        for (bullet in bullets) {
                            val cleanBullet = if (bullet.trim().startsWith("•")) bullet.trim() else "• ${bullet.trim()}"
                            htmlBuilder.append("<p style=\"margin-bottom: 2px;\">$cleanBullet</p>")
                        }
                        htmlBuilder.append("</div></div>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.projects.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Portfolio Projects</div>
                    """)
                    for (proj in resume.projects) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="background-color: $cardBg; padding: 10px; border-radius: 8px; border:1px solid $borderLine; margin-bottom: 8px;">
                            <div style="display:flex; justify-content:space-between; font-weight:bold; color: $textMain;">
                                <span>${proj.title}</span>
                                <span style="font-size:9.5px;">${if (proj.techStack.isNotBlank()) "${proj.techStack}" else ""}</span>
                            </div>
                            <p style="margin-top: 4px; margin-bottom: 4px; color: $textSec;">${proj.description}</p>
                            <div style="font-size:9.5px;">
                                ${if (proj.githubLink.isNotBlank()) "<a href=\"${proj.githubLink}\" target=\"_blank\">Codebase</a>" else ""}
                                ${if (proj.liveLink.isNotBlank()) " &nbsp;|&nbsp; <a href=\"${proj.liveLink}\" target=\"_blank\">Live Demo</a>" else ""}
                            </div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.education.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Education</div>
                    """)
                    for (edu in resume.education) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 8px;">
                            <div style="display:flex; justify-content:space-between; font-weight:bold;">
                                <span style="color: $textMain;">${edu.degree}</span>
                                <span style="font-size:10px; color:$textSec;">${edu.startYear} - ${edu.endYear}</span>
                            </div>
                            <div style="font-size: 10.5px; color: $primaryColor;">${edu.institution}</div>
                            ${if (edu.grade.isNotBlank()) "<div style=\"font-size:10px; color:$textSec;\">Score/Marks: ${edu.grade}</div>" else ""}
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.certifications.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title">Certifications</div>
                    """)
                    for (cert in resume.certifications) {
                        htmlBuilder.append("<p style=\"margin-bottom: 4px; color:$textMain;\"><b>${cert.name}</b> — ${cert.issuer} (${cert.date})</p>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.achievements.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Interests & Awards</div>
                        <p style="color: $textSec;">${resume.achievements}</p>
                    </div>
                    """)
                }

                htmlBuilder.append("</div></div>") // End creative grid container
            }

            "Developer Resume" -> {
                // Template 4 Details: Tech-inspired (Dark Headers, Mono accents, Tech skill tags)
                htmlBuilder.append("""
                <div class="template-developer">
                    <div class="developer-header avoid-break">
                        <h1 style="color: $primaryColor; font-size: 22px; font-family: monospace;">&gt; ${resume.fullName.uppercase()}</h1>
                        <p style="color: #ffffff; padding-left: 2px; font-weight:600;">// ${resume.jobTitle}</p>
                        <div class="contacts-bar" style="color: #9cb4ff;">
                            ${if (resume.email.isNotBlank()) "<span class=\"contact-item\">${resume.email}</span>" else ""}
                            ${if (resume.phone.isNotBlank()) "<span class=\"contact-item\">${resume.phone}</span>" else ""}
                            ${if (resume.location.isNotBlank()) "<span class=\"contact-item\">${resume.location}</span>" else ""}
                            ${if (resume.linkedinUrl.isNotBlank()) "<span class=\"contact-item\"><a href=\"${resume.linkedinUrl}\" style=\"color:#00ff66;\">linkedin</a></span>" else ""}
                            ${if (resume.githubUrl.isNotBlank()) "<span class=\"contact-item\"><a href=\"${resume.githubUrl}\" style=\"color:#00ff66;\">github</a></span>" else ""}
                        </div>
                    </div>
                """)

                if (resume.summary.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: profile_summary</div>
                        <p style="text-align: justify; color: $textMain; line-height:1.5;">${resume.summary}</p>
                    </div>
                    """)
                }

                if (resume.skills.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: technical_skills</div>
                        <div class="skills-grid">
                    """)
                    for (skill in resume.skills) {
                        htmlBuilder.append("<span class=\"tech-tag\" style=\"font-family: monospace; border-color: $primaryColor; border-radius: 2px;\"># ${skill.name} (${skill.level})</span>")
                    }
                    htmlBuilder.append("</div></div>")
                }

                if (resume.projects.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: software_projects</div>
                    """)
                    for (proj in resume.projects) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 12px; border-left: 3px solid $primaryColor; padding-left: 10px;">
                            <div style="font-weight: bold; color: $textMain; font-size: 11.5px;">${proj.title} <span class="monospace-accent" style="color: $primaryColor;">${proj.techStack}</span></div>
                            <p style="color: $textSec; margin: 4px 0 3px 0;">${proj.description}</p>
                            <div style="font-size: 10px;">
                                ${if (proj.githubLink.isNotBlank()) "repo: <a href=\"${proj.githubLink}\" target=\"_blank\">${proj.githubLink}</a>" else ""}
                                ${if (proj.liveLink.isNotBlank() && proj.githubLink.isNotBlank()) " | " else ""}
                                ${if (proj.liveLink.isNotBlank()) "live: <a href=\"${proj.liveLink}\" target=\"_blank\">${proj.liveLink}</a>" else ""}
                            </div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.experience.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: work_experience</div>
                    """)
                    for (exp in resume.experience) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 12px;">
                            <table style="width:100%; border-collapse:collapse;">
                                <tr>
                                    <td style="font-weight:bold; color: $textMain; font-size:11.5px;">* ${exp.role} @ ${exp.company}</td>
                                    <td style="text-align:right; font-family:monospace; color:$primaryColor; font-size:10.5px;">[${exp.startDate} - ${exp.endDate}]</td>
                                </tr>
                            </table>
                            <div style="margin-top: 4px; padding-left: 10px; color:$textSec;">
                        """)
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        for (bullet in bullets) {
                            val cleanBullet = if (bullet.trim().startsWith("•")) bullet.trim() else "• ${bullet.trim()}"
                            htmlBuilder.append("<p style=\"margin-bottom:2px;\">$cleanBullet</p>")
                        }
                        htmlBuilder.append("</div></div>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.education.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: academic_manifest</div>
                    """)
                    for (edu in resume.education) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 8px;">
                            <div style="font-weight:bold; color: $textMain;">${edu.degree} (${edu.startYear} - ${edu.endYear})</div>
                            <div style="color: $textSec; font-size:11px;">${edu.institution} ${if (edu.grade.isNotBlank()) "| Grade: ${edu.grade}" else ""}</div>
                            ${if (edu.achievements.isNotBlank()) "<div style=\"font-size:10px; color:$textSec; font-family:monospace;\">// info: ${edu.achievements}</div>" else ""}
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.certifications.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title" style="font-family: monospace;">:: professional_credentials</div>
                    """)
                    for (cert in resume.certifications) {
                        htmlBuilder.append("<p style=\"margin-bottom:4px; font-family:monospace; color:$textSec;\">&gt; ${cert.name} (Issued: ${cert.issuer} | ${cert.date})</p>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.achievements.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title" style="font-family: monospace;">:: telemetry_achievements</div>
                        <p style="color: $textSec; font-family: monospace;">${resume.achievements}</p>
                    </div>
                    """)
                }

                htmlBuilder.append("</div>") // End developer container
            }

            "Executive Premium" -> {
                // Template 5 Details: Luxury Executive (navy/gold accents, structured high corporate font)
                htmlBuilder.append("""
                <div class="template-executive">
                    <div class="executive-header avoid-break">
                        <div class="executive-name-box">
                            <h1 style="font-size: 24px; font-weight: 700; color: $primaryColor; letter-spacing: 0.5px;">${resume.fullName}</h1>
                            <div style="font-size: 12.5px; font-weight: 600; color: $accentColor; text-transform: uppercase; margin-top: 4px; letter-spacing: 1px;">${resume.jobTitle}</div>
                        </div>
                """)

                if (base64Avatar != null) {
                    htmlBuilder.append("<img src=\"$base64Avatar\" class=\"profile-img-circle\" />")
                } else {
                    htmlBuilder.append("<div class=\"monogram-avatar\">$nameInitials</div>")
                }

                htmlBuilder.append("""
                    </div>
                    
                    <div class="contacts-bar avoid-break" style="border-bottom: 1px double $borderLine; padding-bottom: 8px; margin-bottom: 15px; justify-content: center;">
                        ${if (resume.email.isNotBlank()) "<span class=\"contact-item\">${resume.email}</span>" else ""}
                        ${if (resume.phone.isNotBlank()) "<span class=\"contact-item\">${resume.phone}</span>" else ""}
                        ${if (resume.location.isNotBlank()) "<span class=\"contact-item\">${resume.location}</span>" else ""}
                        ${if (resume.linkedinUrl.isNotBlank()) "<span class=\"contact-item\">LinkedIn</span>" else ""}
                    </div>
                """)

                if (resume.summary.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px; text-align: justify;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Executive Profile</div>
                        <p style="color: $textMain; font-style: italic; line-height: 1.5;">${resume.summary}</p>
                    </div>
                    """)
                }

                if (resume.skills.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Core Competencies</div>
                        <div class="skills-grid">
                    """)
                    for (skill in resume.skills) {
                        htmlBuilder.append("<span class=\"tech-tag\" style=\"font-size:10.5px; border-bottom: 2px solid $accentColor; font-family:serif;\">${skill.name}</span>")
                    }
                    htmlBuilder.append("</div></div>")
                }

                if (resume.experience.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Professional Experience</div>
                    """)
                    for (exp in resume.experience) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 12px;">
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 3.5px;">
                                <tr>
                                    <td style="font-weight: bold; color: $textMain; font-size: 11.5px;">${exp.role}</td>
                                    <td style="text-align: right; color: $primaryColor; font-weight: bold; font-size:11px;">${exp.startDate} - ${exp.endDate}</td>
                                </tr>
                                <tr>
                                    <td style="color: $accentColor; font-style: italic; font-weight: 500; font-size:11px;" colspan="2">${exp.company}</td>
                                </tr>
                            </table>
                            <div style="padding-left: 10px; color: $textSec;">
                        """)
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        for (bullet in bullets) {
                            val cleanBullet = if (bullet.trim().startsWith("•")) bullet.trim() else "• ${bullet.trim()}"
                            htmlBuilder.append("<p style=\"margin-bottom: 3px;\">$cleanBullet</p>")
                        }
                        htmlBuilder.append("</div></div>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.projects.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Key Leadership Projects</div>
                    """)
                    for (proj in resume.projects) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 10px;">
                            <div style="font-weight: bold; color: $textMain;">${proj.title}</div>
                            ${if (proj.techStack.isNotBlank()) "<div style=\"font-size:10px; color:$accentColor; font-style:italic;\">Scope: ${proj.techStack}</div>" else ""}
                            <p style="margin-top: 4px; color: $textSec;">${proj.description}</p>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.education.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Education</div>
                    """)
                    for (edu in resume.education) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 6px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; color: $textMain;">${edu.degree}</td>
                                    <td style="text-align: right; color: $textSec; font-size:11px;">${edu.startYear} - ${edu.endYear}</td>
                                </tr>
                                <tr>
                                    <td style="color:$textSec; font-size:11px;">${edu.institution}</td>
                                    <td style="text-align: right; color:$textSec; font-size:11px;">${if (edu.grade.isNotBlank()) "Grade/GPA: ${edu.grade}" else ""}</td>
                                </tr>
                            </table>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.certifications.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Professional Credentials</div>
                    """)
                    for (cert in resume.certifications) {
                        htmlBuilder.append("<p style=\"margin-bottom:4.5px; color:$textMain;\"><b>${cert.name}</b> — ${cert.issuer} (${cert.date})</p>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.achievements.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title" style="color: $primaryColor; border-bottom: 1px solid $accentColor; padding-bottom: 2px;">Executive Awards & Milestones</div>
                        <p style="color: $textSec;">${resume.achievements}</p>
                    </div>
                    """)
                }

                htmlBuilder.append("</div>") // End executive container
            }

            else -> {
                // Template 1 Details: Modern Professional (Circular avatar left, clean, Two-Column)
                htmlBuilder.append("""
                <div class="template-modern-prof">
                    <div class="modern-prof-sidebar">
                        <div class="avoid-break" style="text-align: center; margin-bottom: 15px;">
                """)

                if (base64Avatar != null) {
                    htmlBuilder.append("<img src=\"$base64Avatar\" class=\"profile-img-circle\" style=\"margin: 0 auto;\" />")
                } else {
                    htmlBuilder.append("<div class=\"monogram-avatar\" style=\"margin: 0 auto;\">$nameInitials</div>")
                }

                htmlBuilder.append("""
                        </div>
                        
                        <div class="avoid-break">
                            <div class="section-title">Contact</div>
                            <p style="font-size: 10.5px; color: $textSec; word-break: break-all;">
                                ${if (resume.email.isNotBlank()) "${resume.email}<br>" else ""}
                                ${if (resume.phone.isNotBlank()) "${resume.phone}<br>" else ""}
                                ${if (resume.location.isNotBlank()) "${resume.location}<br>" else ""}
                                ${if (resume.linkedinUrl.isNotBlank()) "<a href=\"${resume.linkedinUrl}\">LinkedIn</a><br>" else ""}
                                ${if (resume.githubUrl.isNotBlank()) "<a href=\"${resume.githubUrl}\">GitHub</a><br>" else ""}
                                ${if (resume.portfolioUrl.isNotBlank()) "<a href=\"${resume.portfolioUrl}\">Website</a>" else ""}
                            </p>
                        </div>
                        <div class="divider"></div>
                """)

                if (resume.skills.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Expertise</div>
                        <div class="skills-grid">
                    """)
                    for (skill in resume.skills) {
                        htmlBuilder.append("<span class=\"tech-tag\">${skill.name} (${skill.level})</span>")
                    }
                    htmlBuilder.append("</div></div><div class=\"divider\"></div>")
                }

                if (resume.languages.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Languages</div>
                        <p style="font-size: 10.5px; color: $textSec;">
                    """)
                    for (lang in resume.languages) {
                        htmlBuilder.append("${lang.name} (${lang.proficiency})<br>")
                    }
                    htmlBuilder.append("</p></div>")
                }

                htmlBuilder.append("""
                    </div>
                    <div class="modern-prof-main">
                        <div class="avoid-break" style="margin-bottom: 16px;">
                            <h1 style="font-size: 24px; color: $primaryColor; text-transform: uppercase;">${resume.fullName}</h1>
                            <div style="font-size: 13px; font-weight: 600; color: $textSec; margin-top: 2px;">${resume.jobTitle}</div>
                            <div class="divider" style="height: 2px; background-color: $primaryColor;"></div>
                        </div>
                """)

                if (resume.summary.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title">Professional Summary</div>
                        <p style="text-align: justify; color: $textMain;">${resume.summary}</p>
                    </div>
                    """)
                }

                if (resume.experience.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Professional Experience</div>
                    """)
                    for (exp in resume.experience) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 12px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; color: $textMain; font-size:11.5px;">${exp.role}</td>
                                    <td style="text-align: right; color: $textSec; font-size: 11px;">${exp.startDate} - ${exp.endDate}</td>
                                </tr>
                                <tr>
                                    <td style="color: $primaryColor; font-size: 11px;" colspan="2"><b>${exp.company}</b></td>
                                </tr>
                            </table>
                            <div style="padding-left: 10px; margin-top: 3px; color: $textSec;">
                        """)
                        val bullets = exp.description.split("\n").filter { it.isNotBlank() }
                        for (bullet in bullets) {
                            val cleanBullet = if (bullet.trim().startsWith("•")) bullet.trim() else "• ${bullet.trim()}"
                            htmlBuilder.append("<p style=\"margin-bottom: 2px;\">$cleanBullet</p>")
                        }
                        htmlBuilder.append("</div></div>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.projects.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Projects</div>
                    """)
                    for (proj in resume.projects) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 10px;">
                            <div style="font-weight: bold; color: $textMain;">${proj.title} <span style="font-weight:normal; color:$primaryColor; font-size:10px;">[${proj.techStack}]</span></div>
                            <p style="color: $textSec; margin: 3px 0;">${proj.description}</p>
                            <div style="font-size: 9.5px;">
                                ${if (proj.githubLink.isNotBlank()) "<a href=\"${proj.githubLink}\" target=\"_blank\">Code</a>" else ""}
                                ${if (proj.liveLink.isNotBlank() && proj.githubLink.isNotBlank()) " | " else ""}
                                ${if (proj.liveLink.isNotBlank()) "<a href=\"${proj.liveLink}\" target=\"_blank\">Demo</a>" else ""}
                            </div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.education.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div style="margin-bottom: 15px;">
                        <div class="section-title">Education</div>
                    """)
                    for (edu in resume.education) {
                        htmlBuilder.append("""
                        <div class="avoid-break" style="margin-bottom: 8px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="font-weight: bold; color: $textMain;">${edu.degree}</td>
                                    <td style="text-align: right; color: $textSec; font-size: 11px;">${edu.startYear} - ${edu.endYear}</td>
                                </tr>
                            </table>
                            <div style="color: $textSec; font-size:11px;">${edu.institution} ${if (edu.grade.isNotBlank()) "| Grade: ${edu.grade}" else ""}</div>
                        </div>
                        """)
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.certifications.isNotEmpty()) {
                    htmlBuilder.append("""
                    <div class="avoid-break" style="margin-bottom: 15px;">
                        <div class="section-title">Certifications</div>
                    """)
                    for (cert in resume.certifications) {
                        htmlBuilder.append("<p style=\"margin-bottom: 4px; color:$textMain;\"><b>${cert.name}</b> — ${cert.issuer} (${cert.date})</p>")
                    }
                    htmlBuilder.append("</div>")
                }

                if (resume.achievements.isNotBlank()) {
                    htmlBuilder.append("""
                    <div class="avoid-break">
                        <div class="section-title">Activities & Honors</div>
                        <p style="color: $textSec;">${resume.achievements}</p>
                    </div>
                    """)
                }

                htmlBuilder.append("</div></div>") // End modern-prof grid container
            }
        }

        htmlBuilder.append("""
</body>
</html>
""")
        var html = htmlBuilder.toString()
        val sharedCss = """
<style>
  h2, h3, .section-title { break-after: avoid; page-break-after: avoid; }
  li, .entry, .timeline-item, .tech-tag { break-inside: avoid; page-break-inside: avoid; }
  img { max-width: 100%; }
</style>"""
        val previewCss = if (forPreview) """
<meta name="viewport" content="width=794, user-scalable=yes">
<style>
  html { background: ${if (isDarkExport) "#0b0f17" else "#cfd6e0"}; }
  body { width: 794px; margin: 18px auto; padding: ${marginMm}mm; min-height: 1123px;
         box-shadow: 0 8px 28px rgba(0,0,0,.28); }
</style>""" else ""
        html = html.replace("</head>", sharedCss + previewCss + "</head>")
        return html
    }

    /** Maps current and legacy template ids to the five templates the renderer knows. */
    fun normalizeTemplate(raw: String): String = when {
        raw in TemplateNames -> raw
        raw.contains("Developer", true) || raw.contains("Terminal", true) -> "Developer Resume"
        raw.contains("Executive", true) || raw.contains("Corporate", true) -> "Executive Premium"
        raw.contains("Creative", true) -> "Premium Creative"
        raw.contains("Minimal", true) || raw.contains("Sheets", true) || raw.contains("Standard", true) -> "Minimal ATS"
        else -> "Modern Professional"
    }
}
