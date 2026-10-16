package com.lolipp.asthmadiary.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lolipp.asthmadiary.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class AppSettings(
    val language: String = "system",
    val morningEnabled: Boolean = false,
    val eveningEnabled: Boolean = false,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val eveningHour: Int = 20,
    val eveningMinute: Int = 0,
)

class SettingsRepository(private val context: Context) {
    private val KEY_LANG = stringPreferencesKey("language")
    private val KEY_M_EN = booleanPreferencesKey("morning_enabled")
    private val KEY_E_EN = booleanPreferencesKey("evening_enabled")
    private val KEY_M_H = intPreferencesKey("morning_hour")
    private val KEY_M_M = intPreferencesKey("morning_minute")
    private val KEY_E_H = intPreferencesKey("evening_hour")
    private val KEY_E_M = intPreferencesKey("evening_minute")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            language = p[KEY_LANG] ?: LocaleHelper.getSaved(context),
            morningEnabled = p[KEY_M_EN] ?: false,
            eveningEnabled = p[KEY_E_EN] ?: false,
            morningHour = p[KEY_M_H] ?: 8,
            morningMinute = p[KEY_M_M] ?: 0,
            eveningHour = p[KEY_E_H] ?: 20,
            eveningMinute = p[KEY_E_M] ?: 0,
        )
    }

    suspend fun setLanguage(code: String) {
        // Пишем в SharedPreferences — нужно для синхронного чтения в attachBaseContext
        LocaleHelper.save(context, code)
        // Дублируем в DataStore для консистентности
        context.dataStore.edit { it[KEY_LANG] = code }
    }

    suspend fun setMorningEnabled(v: Boolean) = context.dataStore.edit { it[KEY_M_EN] = v }
    suspend fun setEveningEnabled(v: Boolean) = context.dataStore.edit { it[KEY_E_EN] = v }
    suspend fun setMorningTime(h: Int, m: Int) = context.dataStore.edit {
        it[KEY_M_H] = h; it[KEY_M_M] = m
    }
    suspend fun setEveningTime(h: Int, m: Int) = context.dataStore.edit {
        it[KEY_E_H] = h; it[KEY_E_M] = m
    }
}
