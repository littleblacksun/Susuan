package com.xingce.speedmath.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FormulaDao {
    @Query("SELECT * FROM formulas ORDER BY isFavorite DESC, updatedAt DESC, id ASC")
    fun observeAllFormulas(): Flow<List<FormulaEntity>>

    @Query("SELECT COUNT(*) FROM formulas WHERE isBuiltIn = 1")
    fun observeBuiltInCount(): Flow<Int>

    @Query("SELECT title FROM formulas")
    suspend fun getTitles(): List<String>

    @Upsert
    suspend fun upsertFormula(formula: FormulaEntity): Long

    @Insert
    suspend fun insertAll(formulas: List<FormulaEntity>)

    @Query("UPDATE formulas SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("DELETE FROM formulas WHERE id = :id")
    suspend fun deleteById(id: Long)
}
