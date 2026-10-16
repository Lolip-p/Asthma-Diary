package com.lolipp.asthmadiary

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREF = "locale_prefs"
    private const val KEY = "language"

    fun getSaved(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "system") ?: "system"

    fun save(context: Context, code: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, code).apply()
    }

    /** Оборачивает контекст нужной локалью. "system" — возвращает как есть. */
    fun wrap(context: Context): Context {
        val code = getSaved(context)
        if (code == "system") return context
        val locale = when (code) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale("en", "US")
            else -> return context
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
