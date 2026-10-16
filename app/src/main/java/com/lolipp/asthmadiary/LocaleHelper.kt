package com.lolipp.asthmadiary

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    fun apply(code: String) {
        val locales = when (code) {
            "ru", "en" -> LocaleListCompat.forLanguageTags(code)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
