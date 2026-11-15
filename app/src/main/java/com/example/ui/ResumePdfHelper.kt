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
 * PDF export now uses the SAME HTML document as the on-screen preview (ResumeHtmlHelper),
 * so preview, template and exported PDF can no longer drift apart. The old hand-drawn
 * Canvas renderer (a second, different layout engine) has been removed.
 */
object ResumePdfHelper {

    /** Renders the resume HTML off-screen and writes a real A4 PDF file. Callback runs on the main thread. */
    fun exportPdf(context: Context, resume: Resume, dark: Boolean = false, onDone: (File?) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
                val safeName = resume.fullName.ifBlank { "Resume" }.replace(Regex("[^A-Za-z0-9_-]+"), "_")
                val file = File(dir, "${safeName}_Resume.pdf")
                val webView = WebView(context.applicationContext)
                webView.settings.javaScriptEnabled = false
                webView.settings.allowFileAccess = false
                var finished = false
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (finished) return
                        finished = true
                        // give images/fonts a moment to lay out before snapshotting
                        Handler(Looper.getMainLooper()).postDelayed({
                            val attrs = PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                .build()
                            val adapter = webView.createPrintDocumentAdapter(file.nameWithoutExtension)
                            PdfPrint(attrs).print(adapter, file) { ok ->
                                Handler(Looper.getMainLooper()).post {
                                    webView.destroy()
                                    onDone(if (ok && file.length() > 0) file else null)
                                }
                            }
                        }, 450)
                    }
                }
                webView.loadDataWithBaseURL(null, ResumeHtmlHelper.generateHtml(context, resume, dark), "text/html", "utf-8", null)
            } catch (e: Exception) {
                onDone(null)
            }
        }
    }
}
