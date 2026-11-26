package com.xingce.speedmath.ui.practice

import com.xingce.speedmath.data.db.PracticeItemEntity
import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.domain.engine.PracticeSummaryCalculator
import com.xingce.speedmath.domain.model.AnsweredItem
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.ToleranceLevel
import com.xingce.speedmath.domain.model.questionExactDisplay
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

internal fun mapPracticeSession(
    items: List<AnsweredItem>,
    elapsedMs: Long,
    nBackSteps: Int,
    createdAt: Long,
): PracticeSessionDetails {
    val summary = PracticeSummaryCalculator.calculate(items)
    val first = items.first().question
    return PracticeSessionDetails(
        session = PracticeSessionEntity(
            moduleId = first.module.id,
            moduleTitle = first.module.title,
            isEstimation = first.isEstimation,
            toleranceName = first.tolerance.name,
            questionCount = summary.totalCount,
            correctCount = summary.correctCount,
            totalDurationMs = elapsedMs,
            averageErrorPercent = summary.averageAbsErrorPercent,
            nBackSteps = nBackSteps,
            createdAt = createdAt,
        ),
        items = items.map(AnsweredItem::toPracticeItem),
    )
}

internal fun AnsweredItem.toPracticeItem() = PracticeItemEntity(
    questionIndex = question.index,
    expression = question.expression,
    userAnswer = grade.normalizedUserInput,
    exactAnswer = question.exactValue.toPlainString(), // Retain full precision when rebuilding a wrong answer.
    isPercent = question.isPercentAnswer,
    gradeStatus = grade.status.name,
    signedErrorPercent = grade.signedErrorPercent,
    durationMs = durationMs,
)

internal fun formatPracticeTime(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}

internal fun formatPracticeSeconds(durationMs: Long, count: Int = 1): String =
    BigDecimal.valueOf(durationMs.coerceAtLeast(0), 3)
        .divide(BigDecimal.valueOf(count.coerceAtLeast(1).toLong()), 1, RoundingMode.HALF_UP)
        .toPlainString()

internal fun PracticeItemEntity.exactDisplay(module: MathModule): String {
    val recorded = exactWithoutUnit()
    val value = recorded.toBigDecimalOrNull() ?: return recorded
    return questionExactDisplay(module, value)
}

internal fun PracticeItemEntity.exactWithoutUnit(): String = exactAnswer.trim().trimEnd('%', '％').trimEnd()

internal val PracticeSessionEntity.tolerance: ToleranceLevel
    get() = if (isEstimation) ToleranceLevel.fromName(toleranceName) else ToleranceLevel.EXACT
