package com.xingce.speedmath.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.xingce.speedmath.domain.model.MathModule
import java.io.FileOutputStream
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SavedImageResult(
    val uri: Uri,
    val savedToGallery: Boolean,
    val fileName: String,
)

class ImageWorksheetGenerator(private val context: Context) {

    private sealed interface ImageRow {
        data class TwoColumn(val left: String, val right: String?) : ImageRow
        data class FullWidth(val text: String) : ImageRow
    }

    fun generateAndSave(document: WorksheetDocument, module: MathModule): SavedImageResult {
        val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 31f
            typeface = Typeface.MONOSPACE
        }
        val answerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 27f
            typeface = Typeface.DEFAULT
        }

        val cardInnerWidth = IMAGE_WIDTH - OUTER_PADDING * 2 - CARD_PADDING * 2
        val colWidth = (cardInnerWidth - COLUMN_GAP) / 2f
        val rows = buildRows(document, questionPaint, colWidth)
        val answerLines = if (document.includeAnswers) {
            document.questions.mapIndexed { idx, q -> formatAnswerLine(idx + 1, q) }
        } else {
            emptyList()
        }

        val headerHeight = 232f
        val questionCardHeight = CARD_PADDING * 2 + rows.size * QUESTION_ROW_HEIGHT + 12f
        val answerCardHeight = if (answerLines.isNotEmpty()) {
            CARD_PADDING * 2 + 64f + answerLines.size * ANSWER_ROW_HEIGHT
        } else {
            0f
        }
        val totalHeight = (
            OUTER_PADDING +
                headerHeight +
                SECTION_GAP +
                questionCardHeight +
                (if (answerCardHeight > 0f) SECTION_GAP + answerCardHeight else 0f) +
                FOOTER_HEIGHT +
                OUTER_PADDING
            ).toInt().coerceAtLeast(640)

        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            drawWorksheet(
                canvas = canvas,
                document = document,
                rows = rows,
                answerLines = answerLines,
                headerHeight = headerHeight,
                questionCardHeight = questionCardHeight,
                answerCardHeight = answerCardHeight,
                colWidth = colWidth,
                questionPaint = questionPaint,
                answerPaint = answerPaint,
            )
            return saveBitmap(bitmap, module)
        } finally {
            bitmap.recycle()
        }
    }

    private fun buildRows(
        document: WorksheetDocument,
        paint: Paint,
        colWidth: Float,
    ): List<ImageRow> {
        val rows = mutableListOf<ImageRow>()
        var pendingLeft: String? = null
        document.questions.forEachIndexed { index, question ->
            val line = formatQuestionLine(index + 1, question)
            val fullWidth = question.module == MathModule.MULTI_ADD ||
                question.module == MathModule.MIXED_ADD_SUB ||
                paint.measureText(line) > colWidth - 14f
            if (fullWidth) {
                pendingLeft?.let {
                    rows.add(ImageRow.TwoColumn(it, null))
                    pendingLeft = null
                }
                rows.add(ImageRow.FullWidth(line))
            } else if (pendingLeft == null) {
                pendingLeft = line
            } else {
                rows.add(ImageRow.TwoColumn(pendingLeft!!, line))
                pendingLeft = null
            }
        }
        pendingLeft?.let { rows.add(ImageRow.TwoColumn(it, null)) }
        return rows
    }

    private fun drawWorksheet(
        canvas: Canvas,
        document: WorksheetDocument,
        rows: List<ImageRow>,
        answerLines: List<String>,
        headerHeight: Float,
        questionCardHeight: Float,
        answerCardHeight: Float,
        colWidth: Float,
        questionPaint: Paint,
        answerPaint: Paint,
    ) {
        canvas.drawColor(COLOR_BG_SKY_WHITE)

        val left = OUTER_PADDING
        val right = IMAGE_WIDTH - OUTER_PADDING
        var cursorY = OUTER_PADDING

        // Top sky-blue gradient header card
        val headerRect = RectF(left, cursorY, right, cursorY + headerHeight)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left, headerRect.top, headerRect.right, headerRect.bottom,
                COLOR_PRIMARY_LIGHT, COLOR_PRIMARY_DEEP, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(headerRect, CARD_RADIUS, CARD_RADIUS, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = 26f
            typeface = Typeface.DEFAULT
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 24f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(document.title, headerRect.left + 40f, headerRect.top + 78f, titlePaint)
        canvas.drawText(document.subtitle, headerRect.left + 40f, headerRect.top + 132f, subtitlePaint)
        canvas.drawText("用时：____分____秒    正确率：____%", headerRect.left + 40f, headerRect.top + 188f, fillPaint)

        cursorY = headerRect.bottom + SECTION_GAP

        // White rounded paper card for questions
        val questionRect = RectF(left, cursorY, right, cursorY + questionCardHeight)
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(questionRect, CARD_RADIUS, CARD_RADIUS, whiteFill)
        canvas.drawRoundRect(questionRect, CARD_RADIUS, CARD_RADIUS, borderStroke)

        val subtleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_SUBTLE
            strokeWidth = 1.5f
        }
        val contentLeft = questionRect.left + CARD_PADDING
        val rightColX = contentLeft + colWidth + COLUMN_GAP
        var rowY = questionRect.top + CARD_PADDING
        rows.forEachIndexed { index, row ->
            val baseline = rowY + 42f
            when (row) {
                is ImageRow.TwoColumn -> {
                    canvas.drawText(row.left, contentLeft, baseline, questionPaint)
                    row.right?.let { canvas.drawText(it, rightColX, baseline, questionPaint) }
                }
                is ImageRow.FullWidth -> {
                    canvas.drawText(row.text, contentLeft, baseline, questionPaint)
                }
            }
            if (index < rows.lastIndex) {
                val sepY = rowY + QUESTION_ROW_HEIGHT - 10f
                canvas.drawLine(contentLeft, sepY, questionRect.right - CARD_PADDING, sepY, subtleLinePaint)
            }
            rowY += QUESTION_ROW_HEIGHT
        }

        cursorY = questionRect.bottom

        // Optional ice-blue answer card
        if (answerLines.isNotEmpty() && answerCardHeight > 0f) {
            cursorY += SECTION_GAP
            val answerRect = RectF(left, cursorY, right, cursorY + answerCardHeight)
            val iceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ICE_BLUE }
            canvas.drawRoundRect(answerRect, CARD_RADIUS, CARD_RADIUS, iceFill)
            canvas.drawRoundRect(answerRect, CARD_RADIUS, CARD_RADIUS, borderStroke)

            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY_TEXT
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(
                "参考答案与误差区间",
                answerRect.left + CARD_PADDING,
                answerRect.top + CARD_PADDING + 34f,
                sectionTitlePaint,
            )

            var ansY = answerRect.top + CARD_PADDING + 64f
            answerLines.forEach { line ->
                canvas.drawText(line, answerRect.left + CARD_PADDING, ansY + 32f, answerPaint)
                ansY += ANSWER_ROW_HEIGHT
            }
            cursorY = answerRect.bottom
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("行测速算 · 资料分析速算训练", IMAGE_WIDTH / 2f, cursorY + 56f, footerPaint)
    }

    private fun saveBitmap(bitmap: Bitmap, module: MathModule): SavedImageResult {
        val stamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "xingce_${module.id}_$stamp.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/行测速算")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("无法创建系统相册图片记录")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw IOException("图片编码写入失败")
                    }
                } ?: throw IOException("无法打开系统相册输出流")
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                return SavedImageResult(uri = uri, savedToGallery = true, fileName = fileName)
            } catch (error: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw error
            }
        }

        val file = ExportCacheFiles.createFile(context, module, "png")
        try {
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("图片编码写入失败")
                }
            }
        } catch (error: Throwable) {
            runCatching { file.delete() }
            throw error
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return SavedImageResult(uri = uri, savedToGallery = false, fileName = file.name)
    }

    private companion object {
        const val IMAGE_WIDTH = 1080
        const val OUTER_PADDING = 40f
        const val CARD_PADDING = 38f
        const val CARD_RADIUS = 30f
        const val SECTION_GAP = 28f
        const val COLUMN_GAP = 28f
        const val QUESTION_ROW_HEIGHT = 76f
        const val ANSWER_ROW_HEIGHT = 48f
        const val FOOTER_HEIGHT = 86f

        val COLOR_BG_SKY_WHITE = Color.parseColor("#F4F8FD")
        val COLOR_PRIMARY_LIGHT = Color.parseColor("#52A6FF")
        val COLOR_PRIMARY_DEEP = Color.parseColor("#2E84EC")
        val COLOR_PRIMARY_TEXT = Color.parseColor("#2277DF")
        val COLOR_ICE_BLUE = Color.parseColor("#E8F3FF")
        val COLOR_BORDER = Color.parseColor("#D5E6FA")
        val COLOR_BORDER_SUBTLE = Color.parseColor("#E3EEFA")
        val COLOR_TEXT_PRIMARY = Color.parseColor("#162436")
        val COLOR_TEXT_MUTED = Color.parseColor("#7A8FA6")
    }
}
