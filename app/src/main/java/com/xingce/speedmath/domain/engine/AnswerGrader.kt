package com.xingce.speedmath.domain.engine

import com.xingce.speedmath.domain.model.GradeResult
import com.xingce.speedmath.domain.model.GradeStatus
import com.xingce.speedmath.domain.model.Question
import com.xingce.speedmath.domain.model.ToleranceLevel
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object AnswerGrader {
    private val decimalPattern = Regex("""-?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)""")
    private val preciseRatio = BigDecimal("0.005")

    fun parseDecimal(rawInput: String): BigDecimal? = parseNormalized(normalize(rawInput))

    fun grade(question: Question, rawInput: String): GradeResult {
        val normalized = normalize(rawInput)
        val parsed = parseNormalized(normalized)
        val displayWithUnit = question.exactDisplay + if (question.isPercentAnswer) "%" else ""

        fun result(status: GradeStatus, feedback: String, signedError: Double? = null) = GradeResult(
            status = status,
            isCorrect = status != GradeStatus.FAIL,
            normalizedUserInput = normalized,
            parsedUserValue = parsed,
            exactDisplay = question.exactDisplay,
            signedErrorPercent = signedError,
            feedbackText = feedback,
        )

        if (parsed == null) return result(GradeStatus.FAIL, "格式无效 · 精确 $displayWithUnit")

        val exact = question.exactValue
        val difference = parsed.subtract(exact)
        if (!question.isEstimation || question.tolerance == ToleranceLevel.EXACT) {
            return if (difference.signum() == 0) {
                result(GradeStatus.EXACT, "回答正确", 0.0)
            } else {
                result(GradeStatus.FAIL, "回答错误 · 精确 $displayWithUnit")
            }
        }

        val absoluteExact = exact.abs()
        if (absoluteExact.signum() == 0) {
            return if (difference.signum() == 0) {
                result(GradeStatus.EXACT, "精准命中 · 精确 $displayWithUnit · 误差 0.0%", 0.0)
            } else {
                result(GradeStatus.FAIL, "偏差过大 · 精确 $displayWithUnit")
            }
        }

        // Compare the equivalent products to retain every digit at the tolerance boundary.
        val absoluteDifference = difference.abs()
        val tolerance = BigDecimal.valueOf(question.tolerance.ratio)
        val status = when {
            absoluteDifference <= absoluteExact.multiply(preciseRatio) -> GradeStatus.EXACT
            absoluteDifference <= absoluteExact.multiply(tolerance) -> GradeStatus.PASS
            else -> GradeStatus.FAIL
        }
        val signedRatio = difference.divide(absoluteExact, MathContext.DECIMAL128)
        val roundedPercent = signedRatio.movePointRight(2).setScale(1, RoundingMode.HALF_UP)
        val signedError = roundedPercent.toDouble().takeIf { it.isFinite() }
        val prefix = if (roundedPercent.signum() > 0) "+" else ""
        val formattedError = "$prefix${roundedPercent.toPlainString()}%"
        val label = when (status) {
            GradeStatus.EXACT -> "精准命中"
            GradeStatus.PASS -> "估算通过"
            GradeStatus.FAIL -> "偏差过大"
        }
        return result(status, "$label · 精确 $displayWithUnit · 误差 $formattedError", signedError)
    }

    private fun normalize(rawInput: String): String = rawInput.trim()
        .replace('−', '-')
        .replace('–', '-')
        .replace('－', '-')
        .trimEnd { it == '%' || it == '％' || it.isWhitespace() }

    private fun parseNormalized(input: String): BigDecimal? =
        if (decimalPattern.matches(input)) input.toBigDecimalOrNull() else null
}
