package com.xingce.speedmath.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PracticeSessionEntity::class, PracticeItemEntity::class, FormulaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun practiceDao(): PracticeDao
    abstract fun formulaDao(): FormulaDao
}
