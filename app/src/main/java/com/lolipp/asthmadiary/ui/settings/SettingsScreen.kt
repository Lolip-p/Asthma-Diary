package com.lolipp.asthmadiary.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { snackbarHost.showSnackbar(it); snackbarMsg = null }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportBackup { json ->
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                snackbarMsg = context.getString(R.string.settings_backup_export_done, 0)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: ""
            vm.importBackup(text,
                onResult = { n -> snackbarMsg = context.getString(R.string.settings_backup_import_done, n) },
                onError = { err -> snackbarMsg = context.getString(R.string.settings_backup_error, err) }
            )
        }
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_notifications))

            ReminderRow(
                label = stringResource(R.string.settings_notif_morning),
                enabled = s.morningEnabled,
                hour = s.morningHour,
                minute = s.morningMinute,
                onToggle = { en ->
                    if (en && Build.VERSION.SDK_INT >= 33) {
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
                    if (en && Build.VERSION.SDK_INT >= 33) {
                        permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setEvening(en, s.eveningHour, s.eveningMinute)
                },
                onTimeChange = { h, m -> vm.setEvening(s.eveningEnabled, h, m) }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_notif_check_absent),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = s.absentReminderEnabled,
                    onCheckedChange = { vm.setAbsent(it) }
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader(stringResource(R.string.settings_backup))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("asthma_diary_backup.json") },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.settings_backup_export)) }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.settings_backup_import)) }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
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
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
