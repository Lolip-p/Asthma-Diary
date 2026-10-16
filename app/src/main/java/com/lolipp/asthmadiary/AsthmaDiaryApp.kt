package com.lolipp.asthmadiary

import android.app.Application
import android.content.Context

/**
 * Обёртка Application — нужна, чтобы Resources всего процесса
 * (включая WorkManager, нотификации, getString из репозиториев)
 * отдавались с выбранной локалью, а не с системной.
 */
class AsthmaDiaryApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
