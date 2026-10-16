package com.lolipp.asthmadiary

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lolipp.asthmadiary.data.SettingsRepository
import com.lolipp.asthmadiary.ui.add.AddEntryScreen
import com.lolipp.asthmadiary.ui.home.HomeScreen
import com.lolipp.asthmadiary.ui.onboarding.OnboardingScreen
import com.lolipp.asthmadiary.ui.settings.SettingsScreen
import com.lolipp.asthmadiary.ui.theme.AsthmaDiaryTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        val repo = SettingsRepository(applicationContext)

        setContent {
            AsthmaDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var onboardingSeen by remember { mutableStateOf<Boolean?>(null) }
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

                    LaunchedEffect(Unit) {
                        onboardingSeen = repo.settings.first().onboardingSeen
                    }

                    when (onboardingSeen) {
                        null -> {}
                        false -> OnboardingScreen(onFinish = {
                            lifecycleScope.launch { repo.setOnboardingSeen(true) }
                            onboardingSeen = true
                        })
                        true -> {
                            BackHandler(enabled = screen != Screen.Home) {
                                screen = Screen.Home
                            }

                            AnimatedContent(
                                targetState = screen,
                                transitionSpec = {
                                    val enter = fadeIn(tween(200)) + slideInHorizontally(
                                        tween(200), initialOffsetX = { it / 8 }
                                    )
                                    val exit = fadeOut(tween(150)) + slideOutHorizontally(
                                        tween(200), targetOffsetX = { -it / 8 }
                                    )
                                    enter togetherWith exit
                                },
                                label = "screen"
                            ) { s ->
                                when (s) {
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
            }
        }
    }
}
