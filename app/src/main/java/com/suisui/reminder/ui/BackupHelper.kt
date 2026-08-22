package com.suisui.reminder.ui

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.suisui.reminder.data.MomentEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把「瞬间」照片备份到用户选择的文件夹（支持 Google 云盘 / OneDrive / Dropbox 等
 * 通过系统文件选择器暴露的云盘文件夹）。
 */
object BackupHelper {

    data class Result(val images: Int, val skipped: Int, val message: String)

    suspend fun backupMoments(
        context: Context,
        treeUri: Uri,
        moments: List<MomentEntity>
    ): Result = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null) {
            return@withContext Result(0, moments.sumOf { it.imagePaths.size }, "无法访问所选文件夹")
        }
        val dirName = "岁岁提醒备份_" +
            SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val dir = root.createDirectory(dirName)
        if (dir == null) {
            return@withContext Result(0, moments.sumOf { it.imagePaths.size }, "创建备份文件夹失败")
        }
        var images = 0
        var skipped = 0
        moments.forEach { m ->
            m.imagePaths.forEachIndexed { idx, path ->
                val f = File(path)
                if (f.exists()) {
                    val target = dir.createFile("image/jpeg", "moment_${m.id}_$idx.jpg")
                    if (target != null) {
                        val ok = context.contentResolver.openOutputStream(target.uri)?.use { out ->
                            f.inputStream().use { it.copyTo(out) }
                            true
                        } ?: false
                        if (ok) images++ else skipped++
                    } else {
                        skipped++
                    }
                } else {
                    skipped++
                }
            }
        }
        val manifest = dir.createFile("text/plain", "说明.txt")
        manifest?.let { mf ->
            context.contentResolver.openOutputStream(mf.uri)?.use { out ->
                out.write(buildManifest(moments).toByteArray(Charsets.UTF_8))
            }
        }
        Result(images, skipped, "已备份 $images 张照片（跳过 $skipped 张），并写入说明.txt")
    }

    private fun buildManifest(moments: List<MomentEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("岁岁提醒 · 瞬间照片备份清单")
        sb.appendLine("备份时间：" + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        sb.appendLine("----------------------------------")
        moments.forEachIndexed { i, m ->
            sb.appendLine("${i + 1}. ${m.note ?: "(无备注)"}（${m.imagePaths.size} 张照片）")
        }
        return sb.toString()
    }
}
