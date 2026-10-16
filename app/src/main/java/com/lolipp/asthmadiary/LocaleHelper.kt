package com.lolipp.asthmadiary

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    /** Жёстко фиксируем русскую локаль. */
    fun wrap(base: Context): Context {
        val locale = Locale("ru", "RU")
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
