package com.lolipp.asthmadiary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.lolipp.asthmadiary.data.SettingsRepository
import com.lolipp.asthmadiary.ui.add.AddEntryScreen
import com.lolipp.asthmadiary.ui.home.HomeScreen
import com.lolipp.asthmadiary.ui.settings.SettingsScreen
import com.lolipp.asthmadiary.ui.theme.AsthmaDiaryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class AddEntry(val entryId: Long?) : Screen
    data object Settings : Screen
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // применяем сохранённый язык при старте
        lifecycleScope.launch {
            val saved = SettingsRepository(applicationContext).settings.first().language
            LocaleHelper.apply(saved)
        }

        setContent {
            AsthmaDiaryTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                when (val s = screen) {
                    is Screen.Home -> HomeScreen(
                        onAddClick = { screen = Screen.AddEntry(null) },
                        onEditClick = { id -> screen = Screen.AddEntry(id) },
                        onSettingsClick = { screen = Screen.Settings }
                    )
                    is Screen.AddEntry -> AddEntryScreen(
                        entryId = s.entryId,
                        onSaved = { screen = Screen.Home },
                        onCancel = { screen = Screen.Home }
                    )
                    is Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.Home }
                    )
                }
            }
        }
    }
}
