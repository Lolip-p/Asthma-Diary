package com.lolipp.asthmadiary.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lolipp.asthmadiary.data.Period
import com.lolipp.asthmadiary.data.PeakFlowEntry
import com.lolipp.asthmadiary.data.Timing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val maxV by vm.maxValue.collectAsStateWithLifecycle()
    val minV by vm.minValue.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Asthma Diary", fontWeight = FontWeight.SemiBold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить запись") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Рекорд",
                        value = maxV?.toString() ?: "—",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        label = "Минимум",
                        value = minV?.toString() ?: "—",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "График появится тут",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Пока нет записей. Нажми «Добавить запись».",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { e ->
                    EntryRow(e)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                "л/мин",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryRow(e: PeakFlowEntry) {
    val fmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fmt.format(Date(e.timestamp)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                val timing = if (e.timing == Timing.BEFORE) "До" else "После"
                val period = if (e.period == Period.MORNING) "Утро" else "Вечер"
                Text(
                    "$period · $timing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${e.value}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
