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
        val html = ExtraTemplates.render(context, safe, tpl, opts, isDarkExport, mm)

        val extra = StringBuilder()
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

    /** Maps current and legacy template ids to the ten templates the renderer knows. */
    fun normalizeTemplate(raw: String): String = when {
        raw in TemplateNames -> raw
        raw.contains("Developer", true) || raw.contains("Terminal", true) -> "Developer Resume"
        raw.contains("Executive", true) || raw.contains("Corporate", true) -> "Executive Premium"
        raw.contains("Creative", true) -> "Premium Creative"
        raw.contains("Minimal", true) || raw.contains("Sheets", true) || raw.contains("Standard", true) -> "Minimal ATS"
        else -> "Modern Professional"
    }
}
