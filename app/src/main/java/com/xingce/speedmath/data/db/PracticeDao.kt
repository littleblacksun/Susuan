package com.xingce.speedmath.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {
    @Insert
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Insert
    suspend fun insertItems(items: List<PracticeItemEntity>)

    @Transaction
    suspend fun saveSessionWithItems(
        session: PracticeSessionEntity,
        items: List<PracticeItemEntity>,
    ): Long {
        val sessionId = insertSession(session)
        insertItems(items.map { it.copy(sessionId = sessionId) })
        return sessionId
    }

    @Query("SELECT * FROM practice_sessions ORDER BY createdAt DESC, id DESC")
    fun observeAllSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE isEstimation = :isEstimation ORDER BY createdAt DESC, id DESC")
    fun observeSessionsByType(isEstimation: Boolean): Flow<List<PracticeSessionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(questionCount), 0) AS questionCount,
               COALESCE(SUM(correctCount), 0) AS correctCount,
               COALESCE(SUM(totalDurationMs), 0) AS totalDurationMs
        FROM practice_sessions
        WHERE createdAt >= :startOfDayMillis
        """,
    )
    fun observeTodayStats(startOfDayMillis: Long): Flow<StatsAggregate>

    @Query(
        """
        SELECT COALESCE(SUM(questionCount), 0) AS questionCount,
               COALESCE(SUM(correctCount), 0) AS correctCount,
               COALESCE(SUM(totalDurationMs), 0) AS totalDurationMs
        FROM practice_sessions
        """,
    )
    fun observeOverallStats(): Flow<StatsAggregate>

    @Query("SELECT * FROM practice_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): PracticeSessionEntity?

    @Query("SELECT * FROM practice_items WHERE sessionId = :sessionId ORDER BY questionIndex, id")
    suspend fun getItemsBySessionId(sessionId: Long): List<PracticeItemEntity>

    @Transaction
    suspend fun getSessionDetails(sessionId: Long): PracticeSessionDetails? {
        val session = getSessionById(sessionId) ?: return null
        return PracticeSessionDetails(session, getItemsBySessionId(sessionId))
    }

    @Query("DELETE FROM practice_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM practice_sessions")
    suspend fun clearAllSessions()
}
