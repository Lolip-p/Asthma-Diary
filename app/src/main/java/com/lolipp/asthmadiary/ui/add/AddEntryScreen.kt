package com.lolipp.asthmadiary.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lolipp.asthmadiary.data.Period
import com.lolipp.asthmadiary.data.Timing

private const val STEP = 25
private const val DEFAULT_VALUE = 400
private const val MIN_VALUE = 50
private const val MAX_VALUE = 900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    vm: AddEntryViewModel = viewModel()
) {
    var value by remember { mutableIntStateOf(DEFAULT_VALUE) }
    var timing by remember { mutableStateOf(Timing.BEFORE) }
    var period by remember { mutableStateOf(Period.MORNING) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая запись") },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !saving) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            saving = true
                            vm.save(value, timing, period) { onSaved() }
                        },
                        enabled = !saving
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Сохранить",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Timing segmented button
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = timing == Timing.BEFORE,
                    onClick = { timing = Timing.BEFORE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("До ингаляции") }
                SegmentedButton(
                    selected = timing == Timing.AFTER,
                    onClick = { timing = Timing.AFTER },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("После ингаляции") }
            }

            // Period segmented button
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = period == Period.MORNING,
                    onClick = { period = Period.MORNING },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Утро") }
                SegmentedButton(
                    selected = period == Period.EVENING,
                    onClick = { period = Period.EVENING },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Вечер") }
            }

            Spacer(Modifier.height(8.dp))

            // Big value
            Text(
                value.toString(),
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "л/мин · шаг $STEP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // +/- buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { if (value - STEP >= MIN_VALUE) value -= STEP },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "-$STEP", modifier = Modifier.size(32.dp))
                }
                FilledTonalIconButton(
                    onClick = { if (value + STEP <= MAX_VALUE) value += STEP },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+$STEP", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    saving = true
                    vm.save(value, timing, period) { onSaved() }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить", fontSize = 16.sp)
            }
        }
    }
}
