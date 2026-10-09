package com.mit.tushar_kaldate.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mit.tushar_kaldate.data.model.EngineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

class SettingsStore(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_ENGINE = stringPreferencesKey("default_engine")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val RESPONSE_LENGTH = intPreferencesKey("response_length")
        val CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_key")
    }

    val defaultEngine: Flow<EngineType> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val engineName = preferences[PreferencesKeys.DEFAULT_ENGINE]
            engineName?.let {
                try {
                    EngineType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    EngineType.AUTO
                }
            } ?: EngineType.AUTO
        }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val modeName = preferences[PreferencesKeys.THEME_MODE]
            modeName?.let {
                try {
                    ThemeMode.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }
            } ?: ThemeMode.SYSTEM
        }

    val responseLength: Flow<Int> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.RESPONSE_LENGTH] ?: 512
        }

    val customGeminiApiKey: Flow<String?> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CUSTOM_GEMINI_KEY]
        }

    suspend fun setDefaultEngine(engine: EngineType) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ENGINE] = engine.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setResponseLength(length: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.RESPONSE_LENGTH] = length
        }
    }

    suspend fun setCustomApiKey(key: String?) {
        context.settingsDataStore.edit { preferences ->
            if (key != null) {
                preferences[PreferencesKeys.CUSTOM_GEMINI_KEY] = key
            } else {
                preferences.remove(PreferencesKeys.CUSTOM_GEMINI_KEY)
            }
        }
    }

    suspend fun clearSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
