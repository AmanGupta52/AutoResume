package com.example.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PdfPrint
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Resume
import java.io.File

/**
 * PDF export uses the SAME HTML as the on-screen preview (ResumeHtmlHelper), so preview, template
 * and exported PDF cannot drift apart. [exportHtml] is also used for cover letters.
 */
object ResumePdfHelper {

    fun exportPdf(context: Context, resume: Resume, dark: Boolean = false, onDone: (File?) -> Unit) {
        val safe = resume.fullName.ifBlank { "Resume" }.replace(Regex("[^A-Za-z0-9_-]+"), "_")
        Handler(Looper.getMainLooper()).post {
            exportHtml(context, ResumeHtmlHelper.generateHtml(context, resume, dark), "${safe}_Resume", onDone)
        }
    }

    /** Renders HTML off-screen and writes an A4 PDF. Must be called on the main thread; callback runs on main. */
    fun exportHtml(context: Context, html: String, baseName: String, onDone: (File?) -> Unit) {
        try {
            val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
            val file = File(dir, "$baseName.pdf")
            val webView = WebView(context.applicationContext)
            webView.settings.javaScriptEnabled = false
            webView.settings.allowFileAccess = false
            var finished = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (finished) return
                    finished = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        val attrs = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        val adapter = webView.createPrintDocumentAdapter(baseName)
                        PdfPrint(attrs).print(adapter, file) { ok ->
                            Handler(Looper.getMainLooper()).post {
                                webView.destroy()
                                onDone(if (ok && file.length() > 0) file else null)
                            }
                        }
                    }, 450)
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } catch (e: Exception) {
            onDone(null)
        }
    }
}
