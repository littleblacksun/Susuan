package com.xingce.speedmath.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "formulas")
data class FormulaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val expression: String,
    val note: String,
    val isFavorite: Boolean,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
