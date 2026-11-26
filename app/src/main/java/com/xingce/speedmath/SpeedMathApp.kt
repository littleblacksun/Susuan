package com.xingce.speedmath

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.xingce.speedmath.data.db.AppDatabase
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import com.xingce.speedmath.data.repository.FormulaRepository
import com.xingce.speedmath.data.repository.PracticeRepository
import com.xingce.speedmath.export.WorksheetExporter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpeedMathApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.e("SpeedMathApp", "Failed to initialize built-in formulas", error)
        },
    )

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { container.formulaRepository.ensureSeeded() }
    }
}

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "speedmath.db",
    ).build()

    val preferencesStore = UserPreferencesStore(context)
    val formulaRepository = FormulaRepository(database, preferencesStore)
    val practiceRepository = PracticeRepository(database.practiceDao())
    val worksheetExporter = WorksheetExporter(context.applicationContext)
}
