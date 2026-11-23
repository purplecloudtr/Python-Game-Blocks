package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "python_yol_ac_prefs")

data class SavedProgress(
    val currentLevelId: Int = 1,
    val completedLevelIds: Set<Int> = emptySet(),
    val savedCodeByLevel: Map<Int, String> = emptyMap(),
    val largeTextEnabled: Boolean = false,
)

class ProgressRepository(private val context: Context) {

    private object Keys {
        val CURRENT_LEVEL_ID = intPreferencesKey("current_level_id")
        val COMPLETED_LEVELS = stringSetPreferencesKey("completed_levels")
        val LARGE_TEXT = booleanPreferencesKey("large_text_enabled")
        fun codeForLevel(levelId: Int) = stringPreferencesKey("code_level_$levelId")
    }

    val progressFlow: Flow<SavedProgress> = context.dataStore.data.map { prefs ->
        val currentLevelId = prefs[Keys.CURRENT_LEVEL_ID] ?: 1
        val completed = prefs[Keys.COMPLETED_LEVELS]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
        val codeMap = LevelCatalog.levels.mapNotNull { level ->
            prefs[Keys.codeForLevel(level.id)]?.let { code -> level.id to code }
        }.toMap()
        val largeText = prefs[Keys.LARGE_TEXT] ?: false

        SavedProgress(
            currentLevelId = currentLevelId,
            completedLevelIds = completed,
            savedCodeByLevel = codeMap,
            largeTextEnabled = largeText,
        )
    }

    suspend fun setCurrentLevel(levelId: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENT_LEVEL_ID] = levelId
        }
    }

    suspend fun saveCode(levelId: Int, code: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.codeForLevel(levelId)] = code
        }
    }

    suspend fun clearCode(levelId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.codeForLevel(levelId))
        }
    }

    suspend fun markLevelCompleted(levelId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_LEVELS]?.toMutableSet() ?: mutableSetOf()
            current.add(levelId.toString())
            prefs[Keys.COMPLETED_LEVELS] = current
        }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LARGE_TEXT] = enabled
        }
    }
}
