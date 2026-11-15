package android.print

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Lives in android.print on purpose: LayoutResultCallback / WriteResultCallback have
 * package-private constructors, which is the standard way to write a WebView to a PDF file silently.
 */
class PdfPrint(private val attributes: PrintAttributes) {
    fun print(adapter: PrintDocumentAdapter, file: File, onResult: (Boolean) -> Unit) {
        adapter.onLayout(null, attributes, CancellationSignal(), object : PrintDocumentAdapter.LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                try {
                    val pfd = ParcelFileDescriptor.open(
                        file,
                        ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE,
                    )
                    adapter.onWrite(arrayOf(PageRange.ALL_PAGES), pfd, CancellationSignal(), object : PrintDocumentAdapter.WriteResultCallback() {
                        override fun onWriteFinished(pages: Array<out PageRange>?) {
                            try { pfd.close() } catch (_: Exception) {}
                            onResult(true)
                        }
                        override fun onWriteFailed(error: CharSequence?) {
                            try { pfd.close() } catch (_: Exception) {}
                            onResult(false)
                        }
                    })
                } catch (e: Exception) {
                    onResult(false)
                }
            }
            override fun onLayoutFailed(error: CharSequence?) { onResult(false) }
        }, null)
    }
}
