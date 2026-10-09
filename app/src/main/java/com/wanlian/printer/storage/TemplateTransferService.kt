package com.wanlian.printer.storage

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.TemplateTransferRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class TemplateTransferService(context: Context) {
    private val appContext = context.applicationContext

    suspend fun createShareIntent(template: CoupletTemplate, payload: String): Intent =
        withContext(Dispatchers.IO) {
            val exportDirectory = File(appContext.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
            val fileName = "${TemplateTransferRules.safeFileStem(template.name)}." +
                TemplateTransferRules.FILE_EXTENSION
            val exportFile = File(exportDirectory, fileName)
            exportFile.writeText(payload, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                exportFile,
            )
            Intent(Intent.ACTION_SEND).apply {
                type = TemplateTransferRules.MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, fileName)
                clipData = ClipData.newRawUri(fileName, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

    suspend fun readPayload(uri: Uri): String = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("无法读取模板文件")
        input.buffered().use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > TemplateTransferRules.MAX_IMPORT_BYTES) {
                    throw IOException("模板文件超过 2 MB，无法导入")
                }
                output.write(buffer, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    companion object {
        private const val EXPORT_DIRECTORY = "shared_templates"
    }
}
