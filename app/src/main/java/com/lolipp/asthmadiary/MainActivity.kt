package com.lolipp.asthmadiary

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.lolipp.asthmadiary.ui.add.AddEntryScreen
import com.lolipp.asthmadiary.ui.home.HomeScreen
import com.lolipp.asthmadiary.ui.settings.SettingsScreen
import com.lolipp.asthmadiary.ui.theme.AsthmaDiaryTheme

sealed interface Screen {
    data object Home : Screen
    data class AddEntry(val entryId: Long?) : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsthmaDiaryTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                // Кнопка «назад»: из любого экрана → Домой.
                // На главном — стандартное поведение (выход из приложения).
                BackHandler(enabled = screen != Screen.Home) {
                    screen = Screen.Home
                }

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
