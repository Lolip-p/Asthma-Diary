package com.lolipp.asthmadiary.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lolipp.asthmadiary.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Разрешение на уведомления (Android 13+)
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* ignore result, просто запросили */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // Язык
            SectionHeader(stringResource(R.string.settings_language))
            val langs = listOf(
                "system" to stringResource(R.string.settings_language_system),
                "ru" to stringResource(R.string.settings_language_ru),
                "en" to stringResource(R.string.settings_language_en),
            )
            langs.forEach { (code, label) ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = s.language == code,
                        onClick = { vm.setLanguage(code) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }

            HorizontalDivider()

            // Уведомления
            SectionHeader(stringResource(R.string.settings_notifications))

            ReminderRow(
                label = stringResource(R.string.settings_notif_morning),
                enabled = s.morningEnabled,
                hour = s.morningHour,
                minute = s.morningMinute,
                onToggle = { en ->
                    if (en && android.os.Build.VERSION.SDK_INT >= 33) {
                        permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setMorning(en, s.morningHour, s.morningMinute)
                },
                onTimeChange = { h, m -> vm.setMorning(s.morningEnabled, h, m) }
            )

            ReminderRow(
                label = stringResource(R.string.settings_notif_evening),
                enabled = s.eveningEnabled,
                hour = s.eveningHour,
                minute = s.eveningMinute,
                onToggle = { en ->
                    if (en && android.os.Build.VERSION.SDK_INT >= 33) {
                        permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setEvening(en, s.eveningHour, s.eveningMinute)
                },
                onTimeChange = { h, m -> vm.setEvening(s.eveningEnabled, h, m) }
            )

            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_pdf_section))
            Text(
                stringResource(R.string.settings_pdf_hint),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    label: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(
                String.format("%02d:%02d", hour, minute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { showPicker = true }) {
            Text(stringResource(R.string.settings_notif_time))
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(state.hour, state.minute); showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            text = { TimePicker(state = state) }
        )
    }
}


private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
