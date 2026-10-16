package com.lolipp.asthmadiary.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lolipp.asthmadiary.LocaleHelper
import com.lolipp.asthmadiary.data.AppSettings
import com.lolipp.asthmadiary.data.SettingsRepository
import com.lolipp.asthmadiary.notif.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    init {
        Reminders.ensureChannel(app)
    }

    fun setLanguage(code: String) {
        val app = getApplication<Application>()
        // 1) синхронно пишем в SharedPreferences
        LocaleHelper.saveSync(app, code)
        // 2) сообщаем системе (обязательно на API 33+)
        LocaleHelper.applyToSystem(app, code)
        // 3) дожидаемся записи в DataStore, чтобы UI не мигнул старым значением
        runBlocking { repo.setLanguage(code) }
        // 4) жёсткий перезапуск процесса
        LocaleHelper.restartApp(app)
    }

    fun setMorning(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.setMorningEnabled(enabled)
            repo.setMorningTime(hour, minute)
            val ctx = getApplication<Application>()
            if (enabled) Reminders.scheduleMorning(ctx, hour, minute)
            else Reminders.cancelMorning(ctx)
        }
    }

    fun setEvening(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.setEveningEnabled(enabled)
            repo.setEveningTime(hour, minute)
            val ctx = getApplication<Application>()
            if (enabled) Reminders.scheduleEvening(ctx, hour, minute)
            else Reminders.cancelEvening(ctx)
        }
    }
}
