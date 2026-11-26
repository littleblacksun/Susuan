package com.xingce.speedmath.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xingce.speedmath.domain.model.CustomQuizConfig
import com.xingce.speedmath.domain.model.KeypadOrder
import com.xingce.speedmath.domain.model.MathModule
import com.xingce.speedmath.domain.model.NBackLevel
import com.xingce.speedmath.domain.model.ToleranceLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.speedMathDataStore by preferencesDataStore(name = "speedmath_preferences")

data class UserPreferences(
    val lastModule: MathModule = MathModule.FIVE_DIV_THREE,
    val questionCount: Int = 10,
    val tolerance: ToleranceLevel = ToleranceLevel.STANDARD,
    val keypadOrder: KeypadOrder = KeypadOrder.ASCENDING,
    val stylusEnabled: Boolean = true,
    val nBackLevel: NBackLevel = NBackLevel.OFF,
    val hapticEnabled: Boolean = true,
    val customQuizConfig: CustomQuizConfig = CustomQuizConfig(),
    val formulasSeeded: Boolean = false,
    val customQuizConfigured: Boolean = false,
)

class UserPreferencesStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.speedMathDataStore)

    private val defaults = UserPreferences()

    val preferences: Flow<UserPreferences> = dataStore.data.map { readPreferences(it) }

    suspend fun updatePracticePreferences(
        transform: (UserPreferences) -> UserPreferences,
    ): UserPreferences = readPreferences(
        dataStore.edit { values ->
            val updated = transform(readPreferences(values))
            val custom = updated.customQuizConfig.normalized()
            values[Keys.lastModule] = updated.lastModule.id
            values[Keys.questionCount] = updated.questionCount.takeIf { it > 0 } ?: defaults.questionCount
            values[Keys.tolerance] = updated.tolerance.name
            values[Keys.keypadOrder] = updated.keypadOrder.name
            values[Keys.stylusEnabled] = updated.stylusEnabled
            values[Keys.nBackLevel] = updated.nBackLevel.name
            values[Keys.hapticEnabled] = updated.hapticEnabled
            values[Keys.customOperation] = custom.operation.name
            values[Keys.customFirstDigits] = custom.firstDigits
            values[Keys.customSecondDigits] = custom.secondDigits
            values[Keys.customAllowNegative] = custom.allowNegative
            values[Keys.customPercentAnswer] = custom.percentAnswer
            values[Keys.customTolerance] = custom.tolerance.name
            values[Keys.customQuizConfigured] = updated.customQuizConfigured
        },
    )

    internal suspend fun getFormulaSeedVersion(): Int = readFormulaSeedVersion(dataStore.data.first())

    internal suspend fun markFormulaSeedVersion(version: Int) {
        dataStore.edit { values ->
            values[Keys.formulaSeedVersion] = maxOf(version, readFormulaSeedVersion(values))
        }
    }

    private fun readFormulaSeedVersion(values: Preferences): Int =
        values[Keys.formulaSeedVersion] ?: if (values[Keys.formulasSeeded] == true) 1 else 0

    private fun readPreferences(values: Preferences): UserPreferences {
        val defaultCustom = defaults.customQuizConfig
        val custom = CustomQuizConfig(
            operation = readEnum(values[Keys.customOperation], defaultCustom.operation),
            firstDigits = values[Keys.customFirstDigits] ?: defaultCustom.firstDigits,
            secondDigits = values[Keys.customSecondDigits] ?: defaultCustom.secondDigits,
            allowNegative = values[Keys.customAllowNegative] ?: defaultCustom.allowNegative,
            percentAnswer = values[Keys.customPercentAnswer] ?: defaultCustom.percentAnswer,
            tolerance = values[Keys.customTolerance]?.let { ToleranceLevel.fromName(it) }
                ?: defaultCustom.tolerance,
        ).normalized()
        return UserPreferences(
            lastModule = values[Keys.lastModule]?.let { MathModule.fromId(it) } ?: defaults.lastModule,
            questionCount = values[Keys.questionCount]?.takeIf { it > 0 } ?: defaults.questionCount,
            tolerance = values[Keys.tolerance]?.let { ToleranceLevel.fromName(it) } ?: defaults.tolerance,
            keypadOrder = readEnum(values[Keys.keypadOrder], defaults.keypadOrder),
            stylusEnabled = values[Keys.stylusEnabled] ?: defaults.stylusEnabled,
            nBackLevel = readEnum(values[Keys.nBackLevel], defaults.nBackLevel),
            hapticEnabled = values[Keys.hapticEnabled] ?: defaults.hapticEnabled,
            customQuizConfig = custom,
            formulasSeeded = readFormulaSeedVersion(values) > 0,
            customQuizConfigured = values[Keys.customQuizConfigured]
                ?: (custom != defaultCustom || values[Keys.lastModule] == MathModule.CUSTOM.id),
        )
    }

    private inline fun <reified T : Enum<T>> readEnum(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val lastModule = stringPreferencesKey("last_module")
        val questionCount = intPreferencesKey("question_count")
        val tolerance = stringPreferencesKey("tolerance")
        val keypadOrder = stringPreferencesKey("keypad_order")
        val stylusEnabled = booleanPreferencesKey("stylus_enabled")
        val nBackLevel = stringPreferencesKey("n_back_level")
        val hapticEnabled = booleanPreferencesKey("haptic_enabled")
        val customOperation = stringPreferencesKey("custom_operation")
        val customFirstDigits = intPreferencesKey("custom_first_digits")
        val customSecondDigits = intPreferencesKey("custom_second_digits")
        val customAllowNegative = booleanPreferencesKey("custom_allow_negative")
        val customPercentAnswer = booleanPreferencesKey("custom_percent_answer")
        val customTolerance = stringPreferencesKey("custom_tolerance")
        val customQuizConfigured = booleanPreferencesKey("custom_configured")
        val formulasSeeded = booleanPreferencesKey("formulas_seeded")
        val formulaSeedVersion = intPreferencesKey("formula_seed_version")
    }
}
