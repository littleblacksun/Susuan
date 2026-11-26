package com.xingce.speedmath.domain.engine

import com.xingce.speedmath.domain.model.AnsweredItem
import com.xingce.speedmath.domain.model.PracticeSummary
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

object PracticeSummaryCalculator {
    fun calculate(items: List<AnsweredItem>): PracticeSummary {
        if (items.isEmpty()) return PracticeSummary(0, 0, 0, 0L, 0.0, null)

        val count = items.size
        val correct = items.count { it.grade.isCorrect }
        val durationMs = items.sumOf { it.durationMs }
        val averageDurationSec = durationMs.toBigDecimal()
            .divide((count * 1000L).toBigDecimal(), 1, RoundingMode.HALF_UP)
            .toDouble()
        val errors = items.mapNotNull { (question, grade) ->
            grade.signedErrorPercent?.takeIf {
                question.isEstimation && question.tolerance != ToleranceLevel.EXACT && it.isFinite()
            }
        }
        val averageError = if (errors.isEmpty()) null else {
            errors.fold(BigDecimal.ZERO) { total, error -> total.add(error.toBigDecimal().abs()) }
                .divide(errors.size.toBigDecimal(), 1, RoundingMode.HALF_UP)
                .toDouble()
        }
        return PracticeSummary(
            totalCount = count,
            correctCount = correct,
            accuracyPercent = (correct * 100.0 / count).roundToInt(),
            totalDurationMs = durationMs,
            averageDurationSec = averageDurationSec,
            averageAbsErrorPercent = averageError,
        )
    }
}
