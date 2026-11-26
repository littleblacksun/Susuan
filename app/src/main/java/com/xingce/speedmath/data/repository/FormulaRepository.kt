package com.xingce.speedmath.data.repository

import androidx.room.withTransaction
import com.xingce.speedmath.data.db.AppDatabase
import com.xingce.speedmath.data.db.FormulaEntity
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.domain.data.BaifenTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val CURRENT_FORMULA_SEED_VERSION = 2

class FormulaRepository(
    private val database: AppDatabase,
    private val preferencesStore: UserPreferencesStore,
) {
    private val dao = database.formulaDao()
    private val seedMutex = Mutex()

    suspend fun ensureSeeded() = seedMutex.withLock {
        if (preferencesStore.getFormulaSeedVersion() >= CURRENT_FORMULA_SEED_VERSION) return@withLock

        database.withTransaction {
            val titles = dao.getTitles().toHashSet()
            val now = System.currentTimeMillis()
            val missing = BaifenTable.defaultFormulas.filter { it.title !in titles }.map { seed ->
                FormulaEntity(
                    title = seed.title,
                    category = seed.category.name,
                    expression = seed.expression,
                    note = seed.note,
                    isFavorite = seed.isFavorite,
                    isBuiltIn = true,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            if (missing.isNotEmpty()) dao.insertAll(missing)
        }
        // A retry after the Room commit skips the titles that were already inserted.
        preferencesStore.markFormulaSeedVersion(CURRENT_FORMULA_SEED_VERSION)
    }

    fun observeFormulas(): Flow<List<FormulaEntity>> = dao.observeAllFormulas()

    fun observeBuiltInCount(): Flow<Int> = dao.observeBuiltInCount()

    suspend fun saveFormula(formula: FormulaEntity): Long {
        ensureSeeded()
        val now = System.currentTimeMillis()
        val savedId = dao.upsertFormula(
            formula.copy(
                createdAt = if (formula.id == 0L) now else formula.createdAt,
                updatedAt = now,
            ),
        )
        return if (savedId == -1L) formula.id else savedId
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        ensureSeeded()
        dao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    suspend fun deleteFormula(id: Long) {
        ensureSeeded()
        dao.deleteById(id)
    }
}
