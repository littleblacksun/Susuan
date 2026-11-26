package com.xingce.speedmath.domain.model

data class AnsweredItem(val question: Question, val grade: GradeResult, val durationMs: Long)

data class PracticeSummary(
    val totalCount: Int,
    val correctCount: Int,
    val accuracyPercent: Int,
    val totalDurationMs: Long,
    val averageDurationSec: Double,
    val averageAbsErrorPercent: Double?,
)
