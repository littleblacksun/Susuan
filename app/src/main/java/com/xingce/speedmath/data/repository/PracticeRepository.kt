package com.xingce.speedmath.data.repository

import com.xingce.speedmath.data.db.PracticeDao
import com.xingce.speedmath.data.db.PracticeItemEntity
import com.xingce.speedmath.data.db.PracticeSessionDetails
import com.xingce.speedmath.data.db.PracticeSessionEntity
import com.xingce.speedmath.data.db.StatsAggregate
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val dao: PracticeDao) {
    suspend fun saveSessionWithItems(
        session: PracticeSessionEntity,
        items: List<PracticeItemEntity>,
    ): Long = dao.saveSessionWithItems(session, items)

    fun observeAllSessions(): Flow<List<PracticeSessionEntity>> = dao.observeAllSessions()

    fun observeSessionsByType(isEstimation: Boolean): Flow<List<PracticeSessionEntity>> =
        dao.observeSessionsByType(isEstimation)

    fun observeTodayStats(
        startOfDayMillis: Long = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli(),
    ): Flow<StatsAggregate> = dao.observeTodayStats(startOfDayMillis)

    fun observeOverallStats(): Flow<StatsAggregate> = dao.observeOverallStats()

    suspend fun getSessionDetails(sessionId: Long): PracticeSessionDetails? =
        dao.getSessionDetails(sessionId)

    suspend fun deleteSessionById(sessionId: Long) = dao.deleteSessionById(sessionId)

    suspend fun clearAllSessions() = dao.clearAllSessions()
}
