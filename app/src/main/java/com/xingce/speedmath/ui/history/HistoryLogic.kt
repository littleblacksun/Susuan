package com.xingce.speedmath.ui.history

import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.ui.practice.tolerance
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal enum class HistoryFilter(val label: String, val isEstimation: Boolean? = null) {
    ALL("全部"),
    ESTIMATION("估算题型", true),
    EXACT("精确题型", false),
}

internal data class HistoryOverview(val count: String, val accuracy: String, val average: String)

internal fun formatHistoryOverview(stats: StatsAggregate): HistoryOverview {
    val count = NumberFormat.getIntegerInstance(Locale.US).format(stats.questionCount)
    if (stats.questionCount == 0L) return HistoryOverview(count, "—", "—")

    val average = BigDecimal.valueOf(stats.totalDurationMs, 3)
        .divide(BigDecimal.valueOf(stats.questionCount), 1, RoundingMode.HALF_UP)
        .toPlainString()
    return HistoryOverview(count, historyAccuracy(stats.correctCount, stats.questionCount), "${average}秒")
}

internal fun historyAccuracy(correct: Long, count: Long): String =
    if (count > 0) "${(100.0 * correct / count).roundToInt()}%" else "—"

internal fun filterHistorySessions(
    sessions: List<PracticeSessionEntity>,
    filter: HistoryFilter,
): List<PracticeSessionEntity> =
    filter.isEstimation?.let { expected -> sessions.filter { it.isEstimation == expected } } ?: sessions

internal data class HistoryDayGroup(
    val date: LocalDate,
    val title: String,
    val sessions: List<PracticeSessionEntity>,
)

internal fun groupHistorySessions(
    sessions: List<PracticeSessionEntity>,
    zoneId: ZoneId,
    today: LocalDate,
): List<HistoryDayGroup> = sessions
    .sortedWith(compareByDescending<PracticeSessionEntity> { it.createdAt }.thenByDescending { it.id })
    .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zoneId).toLocalDate() }
    .map { (date, group) ->
        val shortDate = "${date.monthValue}月${date.dayOfMonth}日"
        val title = when {
            date == today -> "今天"
            date == today.minusDays(1) -> "昨天"
            date.year == today.year -> shortDate
            else -> "${date.year}年$shortDate"
        }
        HistoryDayGroup(date, title, group)
    }

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

internal fun formatHistoryTime(timestamp: Long, zoneId: ZoneId): String =
    Instant.ofEpochMilli(timestamp).atZone(zoneId).format(timeFormat)

internal fun formatSessionMetadata(session: PracticeSessionEntity, zoneId: ZoneId): String =
    buildList {
        add("${session.questionCount}题")
        if (session.isEstimation) add(session.tolerance.shortLabel)
        if (session.nBackSteps > 0) add("${session.nBackSteps}back")
        add(formatHistoryTime(session.createdAt, zoneId))
    }.joinToString(" · ")
