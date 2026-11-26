package com.xingce.speedmath.domain.model

import java.math.BigDecimal

enum class GradeStatus { EXACT, PASS, FAIL }

data class GradeResult(
    val status: GradeStatus,
    val isCorrect: Boolean,
    val normalizedUserInput: String,
    val parsedUserValue: BigDecimal?,
    val exactDisplay: String,
    val signedErrorPercent: Double?,
    val feedbackText: String,
)
