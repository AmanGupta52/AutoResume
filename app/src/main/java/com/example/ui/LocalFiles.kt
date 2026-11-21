package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * PDFs are never uploaded. They are saved on the phone only:
 * Android 10+ : Downloads/AutoResume (no permission needed)
 * older       : the app's own Documents folder
 */
object LocalFiles {
    /** Returns a short place name to show the person, or null when saving failed. */
    fun savePdf(context: Context, file: File): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AutoResume")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) null
            else {
                resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                "Downloads/AutoResume"
            }
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AutoResume").apply { mkdirs() }
            file.copyTo(File(dir, file.name), overwrite = true)
            "Documents/AutoResume (app folder)"
        }
    } catch (e: Exception) {
        null
    }
}
