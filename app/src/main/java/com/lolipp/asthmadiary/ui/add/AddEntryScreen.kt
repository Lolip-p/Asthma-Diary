package com.lolipp.asthmadiary.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lolipp.asthmadiary.R
import com.lolipp.asthmadiary.data.Period
import com.lolipp.asthmadiary.data.Timing
import java.util.Calendar

private const val STEP = 25
private const val DEFAULT_VALUE = 400
private const val MIN_VALUE = 50
private const val MAX_VALUE = 900

private fun autoPeriod(): Period {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    // до 15:00 считаем "утро", после — "вечер". Пользователь всегда может переключить вручную.
    return if (h < 15) Period.MORNING else Period.EVENING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    entryId: Long?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    vm: AddEntryViewModel = viewModel()
) {
    var value by remember { mutableIntStateOf(DEFAULT_VALUE) }
    var timing by remember { mutableStateOf(Timing.BEFORE) }
    var period by remember { mutableStateOf(autoPeriod()) }
    var saving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(entryId == null) }

    LaunchedEffect(entryId) {
        if (entryId != null) {
            vm.load(entryId) { e ->
                if (e != null) {
                    value = e.value; timing = e.timing; period = e.period
                }
                loaded = true
            }
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val title = if (entryId == null) stringResource(R.string.add_title_new)
                else stringResource(R.string.add_title_edit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !saving) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            saving = true
                            vm.save(entryId, value, timing, period) { onSaved() }
                        },
                        enabled = !saving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = timing == Timing.BEFORE,
                    onClick = { timing = Timing.BEFORE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.add_timing_before)) }
                SegmentedButton(
                    selected = timing == Timing.AFTER,
                    onClick = { timing = Timing.AFTER },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.add_timing_after)) }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = period == Period.MORNING,
                    onClick = { period = Period.MORNING },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(stringResource(R.string.add_period_morning)) }
                SegmentedButton(
                    selected = period == Period.EVENING,
                    onClick = { period = Period.EVENING },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(stringResource(R.string.add_period_evening)) }
            }

            Spacer(Modifier.height(8.dp))

            Text(value.toString(), fontSize = 96.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.add_step_hint, STEP),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { if (value - STEP >= MIN_VALUE) value -= STEP },
                    modifier = Modifier.size(72.dp), shape = CircleShape
                ) { Icon(Icons.Default.Remove, null, Modifier.size(32.dp)) }
                FilledTonalIconButton(
                    onClick = { if (value + STEP <= MAX_VALUE) value += STEP },
                    modifier = Modifier.size(72.dp), shape = CircleShape
                ) { Icon(Icons.Default.Add, null, Modifier.size(32.dp)) }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    saving = true
                    vm.save(entryId, value, timing, period) { onSaved() }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_save), fontSize = 16.sp)
            }
        }
    }
}
