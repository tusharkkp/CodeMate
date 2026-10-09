package com.mit.tushar_kaldate.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionStore(private val context: Context) {

    private object PreferencesKeys {
        val USER_ID = longPreferencesKey("user_id")
        val IS_GUEST = booleanPreferencesKey("is_guest")
    }

    val userId: Flow<Long?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_ID]
        }

    val isGuest: Flow<Boolean> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_GUEST] ?: false
        }

    val isLoggedIn: Flow<Boolean> = userId.map { it != null }

    suspend fun setSession(userId: Long, isGuest: Boolean = false) {
        context.sessionDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.IS_GUEST] = isGuest
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.IS_GUEST)
        }
    }
}
