package com.xingce.speedmath.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.xingce.speedmath.domain.model.MathModule
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object ExportCacheFiles {
    private const val EXPORTS_DIR = "exports"
    private const val MAX_CACHED_FILES = 10
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun exportsDir(context: Context): File = File(context.cacheDir, EXPORTS_DIR).apply {
        if (!exists()) mkdirs()
    }

    fun createFile(context: Context, module: MathModule, extension: String): File {
        val dir = exportsDir(context)
        pruneOldFiles(dir, MAX_CACHED_FILES - 1)
        val stamp = ZonedDateTime.now().format(timestampFormatter)
        return File(dir, "xingce_${module.id}_$stamp.$extension")
    }

    fun pruneOldFiles(dir: File, keepCount: Int) {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        if (files.size <= keepCount) return
        files.sortedByDescending { it.lastModified() }
            .drop(keepCount.coerceAtLeast(0))
            .forEach { runCatching { it.delete() } }
    }
}

class PdfWorksheetGenerator(private val context: Context) {

    private sealed interface LayoutRow {
        data class TwoColumn(val left: String, val right: String?) : LayoutRow
        data class FullWidth(val text: String) : LayoutRow
    }

    private sealed interface PagePlan {
        data class QuestionPage(
            val isFirstPage: Boolean,
            val rows: List<LayoutRow>,
        ) : PagePlan

        data class AnswerPage(
            val isFirstAnswerPage: Boolean,
            val lines: List<String>,
        ) : PagePlan
    }

    fun generate(document: WorksheetDocument, module: MathModule): File {
        val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
        }
        val answerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 10.5f
            typeface = Typeface.DEFAULT
        }

        val pages = buildPagePlans(document, questionPaint)
        val totalPages = pages.size.coerceAtLeast(1)
        val pdfDocument = PdfDocument()
        val outputFile = ExportCacheFiles.createFile(context, module, "pdf")

        try {
            pages.forEachIndexed { pageIndex, plan ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                try {
                    when (plan) {
                        is PagePlan.QuestionPage -> drawQuestionPage(
                            canvas = page.canvas,
                            document = document,
                            plan = plan,
                            questionPaint = questionPaint,
                        )
                        is PagePlan.AnswerPage -> drawAnswerPage(
                            canvas = page.canvas,
                            document = document,
                            plan = plan,
                            answerPaint = answerPaint,
                        )
                    }
                    drawFooter(page.canvas, pageIndex + 1, totalPages)
                } finally {
                    pdfDocument.finishPage(page)
                }
            }

            FileOutputStream(outputFile).use { stream ->
                pdfDocument.writeTo(stream)
            }
        } catch (error: Throwable) {
            runCatching { outputFile.delete() }
            throw error
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    private fun buildPagePlans(document: WorksheetDocument, questionPaint: Paint): List<PagePlan> {
        val colWidth = (CONTENT_WIDTH - COLUMN_GAP) / 2f
        val rows = buildQuestionRows(document, questionPaint, colWidth)

        val pages = mutableListOf<PagePlan>()
        var rowIndex = 0
        var isFirstPage = true
        while (rowIndex < rows.size || isFirstPage) {
            val topY = if (isFirstPage) FIRST_PAGE_QUESTIONS_TOP else NEXT_PAGE_CONTENT_TOP
            val maxRows = ((CONTENT_BOTTOM - topY) / QUESTION_ROW_HEIGHT).toInt().coerceAtLeast(1)
            val slice = if (rowIndex < rows.size) {
                rows.subList(rowIndex, minOf(rowIndex + maxRows, rows.size))
            } else {
                emptyList()
            }
            pages.add(PagePlan.QuestionPage(isFirstPage = isFirstPage, rows = slice))
            rowIndex += slice.size
            isFirstPage = false
            if (rows.isEmpty()) break
        }

        if (document.includeAnswers && document.questions.isNotEmpty()) {
            val answerLines = document.questions.mapIndexed { idx, q -> formatAnswerLine(idx + 1, q) }
            var lineIndex = 0
            var isFirstAnswer = true
            while (lineIndex < answerLines.size) {
                val topY = if (isFirstAnswer) ANSWER_FIRST_PAGE_TOP else NEXT_PAGE_CONTENT_TOP
                val maxLines = ((CONTENT_BOTTOM - topY) / ANSWER_ROW_HEIGHT).toInt().coerceAtLeast(1)
                val slice = answerLines.subList(lineIndex, minOf(lineIndex + maxLines, answerLines.size))
                pages.add(PagePlan.AnswerPage(isFirstAnswerPage = isFirstAnswer, lines = slice))
                lineIndex += slice.size
                isFirstAnswer = false
            }
        }

        return pages
    }

    private fun buildQuestionRows(
        document: WorksheetDocument,
        paint: Paint,
        colWidth: Float,
    ): List<LayoutRow> {
        val rows = mutableListOf<LayoutRow>()
        var pendingLeft: String? = null
        document.questions.forEachIndexed { index, question ->
            val line = formatQuestionLine(index + 1, question)
            val needsFullWidth = question.module == MathModule.MULTI_ADD ||
                question.module == MathModule.MIXED_ADD_SUB ||
                paint.measureText(line) > colWidth - 10f

            if (needsFullWidth) {
                pendingLeft?.let {
                    rows.add(LayoutRow.TwoColumn(it, null))
                    pendingLeft = null
                }
                rows.add(LayoutRow.FullWidth(line))
            } else if (pendingLeft == null) {
                pendingLeft = line
            } else {
                rows.add(LayoutRow.TwoColumn(pendingLeft!!, line))
                pendingLeft = null
            }
        }
        pendingLeft?.let { rows.add(LayoutRow.TwoColumn(it, null)) }
        return rows
    }

    private fun drawQuestionPage(
        canvas: Canvas,
        document: WorksheetDocument,
        plan: PagePlan.QuestionPage,
        questionPaint: Paint,
    ) {
        val startY: Float
        if (plan.isFirstPage) {
            drawMainHeader(canvas, document)
            startY = FIRST_PAGE_QUESTIONS_TOP
        } else {
            drawContinuationHeader(canvas, document.title)
            startY = NEXT_PAGE_CONTENT_TOP
        }

        val colWidth = (CONTENT_WIDTH - COLUMN_GAP) / 2f
        val rightX = MARGIN + colWidth + COLUMN_GAP
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_SUBTLE
            strokeWidth = 0.7f
        }

        var y = startY
        plan.rows.forEach { row ->
            val baseline = y + 18f
            when (row) {
                is LayoutRow.TwoColumn -> {
                    canvas.drawText(row.left, MARGIN, baseline, questionPaint)
                    row.right?.let { canvas.drawText(it, rightX, baseline, questionPaint) }
                }
                is LayoutRow.FullWidth -> {
                    canvas.drawText(row.text, MARGIN, baseline, questionPaint)
                }
            }
            val lineY = y + QUESTION_ROW_HEIGHT - 8f
            canvas.drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, dividerPaint)
            y += QUESTION_ROW_HEIGHT
        }
    }

    private fun drawMainHeader(canvas: Canvas, document: WorksheetDocument) {
        val bannerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 72f)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bannerRect.left, bannerRect.top, bannerRect.right, bannerRect.bottom,
                COLOR_PRIMARY_LIGHT, COLOR_PRIMARY_DEEP, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bannerRect, 12f, 12f, bannerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = 10f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(document.title, bannerRect.left + 16f, bannerRect.top + 30f, titlePaint)
        canvas.drawText(document.subtitle, bannerRect.left + 16f, bannerRect.top + 52f, subtitlePaint)

        val metaY = bannerRect.bottom + 24f
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 10.5f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("用时：______ 分 ______ 秒      正确率：______ %", MARGIN + 4f, metaY, metaPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, metaY + 12f, PAGE_WIDTH - MARGIN, metaY + 12f, linePaint)
    }

    private fun drawContinuationHeader(canvas: Canvas, title: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(title, MARGIN, MARGIN + 14f, paint)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN, MARGIN + 22f, PAGE_WIDTH - MARGIN, MARGIN + 22f, linePaint)
    }

    private fun drawAnswerPage(
        canvas: Canvas,
        document: WorksheetDocument,
        plan: PagePlan.AnswerPage,
        answerPaint: Paint,
    ) {
        val startY: Float
        if (plan.isFirstAnswerPage) {
            val headerRect = RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 48f)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ICE_BLUE }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(headerRect, 10f, 10f, bgPaint)
            canvas.drawRoundRect(headerRect, 10f, 10f, borderPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY_TEXT
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_SECONDARY
                textSize = 9.5f
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("参考答案与误差区间", headerRect.left + 14f, headerRect.top + 22f, titlePaint)
            canvas.drawText(document.title, headerRect.left + 14f, headerRect.top + 38f, subPaint)
            startY = ANSWER_FIRST_PAGE_TOP
        } else {
            drawContinuationHeader(canvas, "${document.title} · 参考答案")
            startY = NEXT_PAGE_CONTENT_TOP
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER_SUBTLE
            strokeWidth = 0.6f
        }

        var y = startY
        plan.lines.forEach { line ->
            canvas.drawText(line, MARGIN + 4f, y + 15f, answerPaint)
            canvas.drawLine(MARGIN, y + ANSWER_ROW_HEIGHT - 3f, PAGE_WIDTH - MARGIN, y + ANSWER_ROW_HEIGHT - 3f, dividerPaint)
            y += ANSWER_ROW_HEIGHT
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("第 $pageNumber / $totalPages 页", PAGE_WIDTH / 2f, PAGE_HEIGHT - 22f, footerPaint)
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 36f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        const val COLUMN_GAP = 18f
        const val FIRST_PAGE_QUESTIONS_TOP = MARGIN + 72f + 44f
        const val NEXT_PAGE_CONTENT_TOP = MARGIN + 34f
        const val ANSWER_FIRST_PAGE_TOP = MARGIN + 48f + 16f
        const val CONTENT_BOTTOM = PAGE_HEIGHT - 44f
        const val QUESTION_ROW_HEIGHT = 48f
        const val ANSWER_ROW_HEIGHT = 24f

        val COLOR_PRIMARY_LIGHT = Color.parseColor("#52A6FF")
        val COLOR_PRIMARY_DEEP = Color.parseColor("#2E84EC")
        val COLOR_PRIMARY_TEXT = Color.parseColor("#2277DF")
        val COLOR_ICE_BLUE = Color.parseColor("#E8F3FF")
        val COLOR_BORDER = Color.parseColor("#D5E6FA")
        val COLOR_BORDER_SUBTLE = Color.parseColor("#E3EEFA")
        val COLOR_TEXT_PRIMARY = Color.parseColor("#162436")
        val COLOR_TEXT_SECONDARY = Color.parseColor("#5D7188")
        val COLOR_TEXT_MUTED = Color.parseColor("#7A8FA6")
    }
}
