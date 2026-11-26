package com.xingce.speedmath.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: String,
    val moduleTitle: String,
    val isEstimation: Boolean,
    val toleranceName: String,
    val questionCount: Int,
    val correctCount: Int,
    val totalDurationMs: Long,
    val averageErrorPercent: Double?,
    val nBackSteps: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "practice_items",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class PracticeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long = 0,
    val questionIndex: Int,
    val expression: String,
    val userAnswer: String,
    val exactAnswer: String,
    val isPercent: Boolean,
    val gradeStatus: String,
    val signedErrorPercent: Double?,
    val durationMs: Long,
)

data class StatsAggregate(
    val questionCount: Long,
    val correctCount: Long,
    val totalDurationMs: Long,
)

data class PracticeSessionDetails(
    val session: PracticeSessionEntity,
    val items: List<PracticeItemEntity>,
)
