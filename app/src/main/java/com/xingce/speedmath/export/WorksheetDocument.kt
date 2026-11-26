package com.xingce.speedmath.export

import com.xingce.speedmath.domain.engine.QuestionGenerator
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ExportFormat(val label: String) {
    PDF("PDF 打印卷"),
    TEXT("纯文本复制"),
    IMAGE("高清长图"),
}

data class ExportRequest(
    val module: MathModule,
    val count: Int,
    val tolerance: ToleranceLevel,
    val customConfig: CustomQuizConfig,
    val format: ExportFormat,
    val includeAnswers: Boolean,
)

data class WorksheetDocument(
    val title: String,
    val subtitle: String,
    val questions: List<Question>,
    val includeAnswers: Boolean,
    val generatedAtLabel: String,
)

object WorksheetBuilder {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun resolveTolerance(
        module: MathModule,
        tolerance: ToleranceLevel,
        customConfig: CustomQuizConfig,
    ): ToleranceLevel = when {
        module == MathModule.CUSTOM -> customConfig.tolerance
        !module.isEstimation -> ToleranceLevel.EXACT
        tolerance == ToleranceLevel.EXACT -> ToleranceLevel.STANDARD
        else -> tolerance
    }

    fun build(
        request: ExportRequest,
        generator: QuestionGenerator = QuestionGenerator(),
        now: ZonedDateTime = ZonedDateTime.now(),
    ): WorksheetDocument {
        val safeCount = request.count.coerceIn(1, 200)
        val effectiveTolerance = resolveTolerance(request.module, request.tolerance, request.customConfig)
        val questions = generator.generateBatch(
            module = request.module,
            count = safeCount,
            tolerance = effectiveTolerance,
            customConfig = request.customConfig,
        )
        val dateLabel = now.format(dateFormatter)
        val generatedAt = now.format(dateTimeFormatter)
        return WorksheetDocument(
            title = "行测速算 · ${request.module.title} 练习卷",
            subtitle = "题量：${questions.size}题 · 误差标准：${effectiveTolerance.label} · 日期：$dateLabel",
            questions = questions,
            includeAnswers = request.includeAnswers,
            generatedAtLabel = generatedAt,
        )
    }
}
