package com.lolipp.asthmadiary

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREF = "locale_prefs"
    private const val KEY = "language"

    fun getSaved(context: Context): String =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, "system") ?: "system"

    /** Синхронная запись — важно, потому что при рестарте новый процесс читает сразу. */
    fun saveSync(context: Context, code: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, code).commit()
    }

    /** Оборачиваем контекст. "system" — отдаём как есть. */
    fun wrap(base: Context): Context {
        val code = getSaved(base)
        if (code == "system") return base
        val locale = when (code) {
            "ru" -> Locale("ru", "RU")
            "en" -> Locale("en", "US")
            else -> return base
        }
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Полный перезапуск приложения.
     * FLAG_ACTIVITY_CLEAR_TASK убивает весь текущий стек,
     * FLAG_ACTIVITY_NEW_TASK создаёт новый task → новый Activity → attachBaseContext
     * с уже сохранённой локалью.
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(intent)
    }
}
