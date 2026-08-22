package com.suisui.reminder.ui

import android.content.Context
import android.net.Uri
import java.io.File
import kotlin.random.Random

object ImageStore {
    fun copyUriToStorage(context: Context, uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "moments").apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${Random.nextInt(100000)}.jpg"
            val dest = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            dest.absolutePath
        }.getOrNull()
    }

    fun deleteImage(path: String) {
        runCatching { File(path).delete() }
    }
}
