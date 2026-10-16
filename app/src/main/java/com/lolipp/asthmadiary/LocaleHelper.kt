package com.lolipp.asthmadiary

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import kotlin.system.exitProcess

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
            "ru" -> Locale.forLanguageTag("ru-RU")
            "en" -> Locale.forLanguageTag("en-US")
            else -> return base
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /**
     * Сообщаем ОС выбранную локаль. С API 33 это критично:
     * без этого система может перетереть нашу локаль системной
     * при пересоздании процесса → UI остаётся английским.
     */
    fun applyToSystem(context: Context, code: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java) ?: return
            lm.applicationLocales = if (code == "system") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList(Locale.forLanguageTag(code))
            }
        }
    }

    /**
     * Жёсткий перезапуск процесса. Гарантирует, что attachBaseContext
     * вызовется заново и кэши Resources/Locale не переиспользуются.
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(intent)
        exitProcess(0)
    }
}
