package com.xingce.speedmath.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.xingce.speedmath.domain.engine.QuestionGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ExportOutcome {
    data class CopiedText(val count: Int) : ExportOutcome
    data class PdfReady(val uri: Uri, val fileName: String) : ExportOutcome
    data class ImageSaved(val uri: Uri, val savedToGallery: Boolean, val fileName: String) : ExportOutcome
    data class Failed(val message: String) : ExportOutcome
}

class WorksheetExporter(
    private val context: Context,
    private val pdfGenerator: PdfWorksheetGenerator = PdfWorksheetGenerator(context),
    private val imageGenerator: ImageWorksheetGenerator = ImageWorksheetGenerator(context),
) {
    suspend fun export(request: ExportRequest): ExportOutcome = withContext(Dispatchers.IO) {
        try {
            val document = WorksheetBuilder.build(request, QuestionGenerator())
            when (request.format) {
                ExportFormat.TEXT -> {
                    val text = TextWorksheetFormatter.format(document)
                    withContext(Dispatchers.Main) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            ?: throw IllegalStateException("系统剪贴板不可用")
                        clipboard.setPrimaryClip(ClipData.newPlainText(document.title, text))
                    }
                    ExportOutcome.CopiedText(document.questions.size)
                }
                ExportFormat.PDF -> {
                    val file = pdfGenerator.generate(document, request.module)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    ExportOutcome.PdfReady(uri = uri, fileName = file.name)
                }
                ExportFormat.IMAGE -> {
                    val saved = imageGenerator.generateAndSave(document, request.module)
                    ExportOutcome.ImageSaved(
                        uri = saved.uri,
                        savedToGallery = saved.savedToGallery,
                        fileName = saved.fileName,
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ExportOutcome.Failed(error.message?.takeIf { it.isNotBlank() } ?: "导出失败，请重试")
        }
    }

    companion object {
        fun shareFile(
            context: Context,
            uri: Uri,
            mimeType: String,
            chooserTitle: String,
        ): Boolean = runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(chooserTitle, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}
