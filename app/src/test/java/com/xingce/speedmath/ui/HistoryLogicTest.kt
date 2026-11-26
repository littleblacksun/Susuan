package com.xingce.speedmath.ui

import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.data.db.StatsAggregate
import com.xingce.speedmath.ui.history.HistoryFilter
import com.xingce.speedmath.ui.history.HistoryOverview
import com.xingce.speedmath.ui.history.filterHistorySessions
import com.xingce.speedmath.ui.history.formatHistoryOverview
import com.xingce.speedmath.ui.history.formatHistoryTime
import com.xingce.speedmath.ui.history.formatSessionMetadata
import com.xingce.speedmath.ui.history.groupHistorySessions
import com.xingce.speedmath.ui.history.historyAccuracy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryLogicTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun overviewHandlesEmptyTotalsAndFormatsCountPercentageAndAverage() {
        assertEquals(HistoryOverview("0", "—", "—"), formatHistoryOverview(StatsAggregate(0, 0, 0)))
        assertEquals(
            HistoryOverview("1,280", "92%", "6.4秒"),
            formatHistoryOverview(StatsAggregate(1_280, 1_178, 8_192_000)),
        )
        assertEquals(HistoryOverview("2", "0%", "0.0秒"), formatHistoryOverview(StatsAggregate(2, 0, 0)))
        assertEquals("90%", historyAccuracy(9, 10))
    }

    @Test
    fun filterUsesTheSessionTypeIncludingCustomSessions() {
        val estimation = session(1, "2026-11-25T16:05:00Z", isEstimation = true)
        val exact = estimation.copy(id = 2, isEstimation = false)
        val sessions = listOf(estimation, exact)
        assertEquals(sessions, filterHistorySessions(sessions, HistoryFilter.ALL))
        assertEquals(listOf(estimation), filterHistorySessions(sessions, HistoryFilter.ESTIMATION))
        assertEquals(listOf(exact), filterHistorySessions(sessions, HistoryFilter.EXACT))
    }

    @Test
    fun groupsByLocalDateWithNewestSessionsFirst() {
        val today = session(5, "2026-11-25T16:05:00Z")
        val later = session(6, "2026-11-26T01:20:00Z")
        val yesterday = session(4, "2026-11-25T15:59:00Z")
        val sameYear = session(3, "2026-06-03T23:00:00Z")
        val pastYear = session(2, "2025-12-30T18:00:00Z")
        val groups = groupHistorySessions(
            listOf(today, pastYear, later, sameYear, yesterday),
            zone,
            LocalDate.of(2026, 11, 26),
        )
        assertEquals(listOf("今天", "昨天", "6月4日", "2025年12月31日"), groups.map { it.title })
        assertEquals(listOf(listOf(later, today), listOf(yesterday), listOf(sameYear), listOf(pastYear)), groups.map { it.sessions })
        assertEquals(
            "昨天",
            groupHistorySessions(listOf(pastYear), zone, LocalDate.of(2026, 1, 1)).single().title,
        )
    }

    @Test
    fun metadataFormatsLocalTimeAndOnlyIncludesEnabledOptions() {
        val estimation = session(1, "2026-11-26T01:04:00Z", isEstimation = true)
        assertEquals("09:04", formatHistoryTime(estimation.createdAt, zone))
        assertEquals("10题 · ±2% · 09:04", formatSessionMetadata(estimation, zone))
        assertEquals("10题 · ±2% · 2back · 09:04", formatSessionMetadata(estimation.copy(nBackSteps = 2), zone))
        assertEquals("10题 · 1back · 09:04", formatSessionMetadata(estimation.copy(isEstimation = false, nBackSteps = 1), zone))
    }

    private fun session(id: Long, time: String, isEstimation: Boolean = false) = PracticeSessionEntity(
        id = id, moduleId = "custom", moduleTitle = "自定义题型", isEstimation = isEstimation,
        toleranceName = "STANDARD", questionCount = 10, correctCount = 9, totalDurationMs = 72_000,
        averageErrorPercent = null, nBackSteps = 0, createdAt = Instant.parse(time).toEpochMilli(),
    )
}
