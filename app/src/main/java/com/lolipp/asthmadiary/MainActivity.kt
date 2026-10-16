package com.lolipp.asthmadiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.lolipp.asthmadiary.ui.add.AddEntryScreen
import com.lolipp.asthmadiary.ui.home.HomeScreen
import com.lolipp.asthmadiary.ui.theme.AsthmaDiaryTheme

sealed interface Screen {
    data object Home : Screen
    data object AddEntry : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsthmaDiaryTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                when (val s = screen) {
                    is Screen.Home -> HomeScreen(onAddClick = { screen = Screen.AddEntry })
                    is Screen.AddEntry -> AddEntryScreen(
                        onSaved = { screen = Screen.Home },
                        onCancel = { screen = Screen.Home }
                    )
                }
            }
        }
    }
}
