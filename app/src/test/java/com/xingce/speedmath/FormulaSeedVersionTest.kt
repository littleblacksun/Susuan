package com.xingce.speedmath

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.xingce.speedmath.data.prefs.UserPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FormulaSeedVersionTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun legacyMarkerMapsToVersionOneAndNewVersionTakesPrecedence() = runTest {
        val file = temporaryFolder.newFolder().resolve("test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val store = UserPreferencesStore(dataStore)
        assertEquals(0, store.getFormulaSeedVersion())
        assertFalse(store.preferences.first().formulasSeeded)

        dataStore.edit { it[booleanPreferencesKey("formulas_seeded")] = true }
        assertEquals(1, store.getFormulaSeedVersion())

        store.markFormulaSeedVersion(2)
        store.updatePracticePreferences { it.copy(questionCount = 20) }
        store.markFormulaSeedVersion(1)
        assertEquals(2, UserPreferencesStore(dataStore).getFormulaSeedVersion())
        assertEquals(20, store.preferences.first().questionCount)
        assertTrue(store.preferences.first().formulasSeeded)
    }
}
