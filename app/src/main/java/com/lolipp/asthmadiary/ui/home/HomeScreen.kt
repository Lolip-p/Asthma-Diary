package com.lolipp.asthmadiary.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lolipp.asthmadiary.R
import com.lolipp.asthmadiary.data.PeakFlowEntry
import com.lolipp.asthmadiary.data.Period
import com.lolipp.asthmadiary.data.Timing
import com.lolipp.asthmadiary.pdf.PdfExporter
import com.lolipp.asthmadiary.ui.chart.PeakFlowChart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChartPeriod { Week, Month }

private fun filterByPeriod(entries: List<PeakFlowEntry>, p: ChartPeriod): List<PeakFlowEntry> {
    val now = System.currentTimeMillis()
    val cutoff = when (p) {
        ChartPeriod.Week -> now - 7L * 24 * 60 * 60 * 1000
        ChartPeriod.Month -> now - 30L * 24 * 60 * 60 * 1000
    }
    return entries.filter { it.timestamp >= cutoff }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val maxV by vm.maxValue.collectAsStateWithLifecycle()
    val minV by vm.minValue.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<PeakFlowEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {
                        PdfExporter.buildAndShare(context, entries, maxV, minV)
                    }, enabled = entries.isNotEmpty()) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share_pdf))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_entry)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        stringResource(R.string.home_personal_best),
                        maxV?.toString() ?: "—",
                        Modifier.weight(1f),
                        MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        stringResource(R.string.home_minimum),
                        minV?.toString() ?: "—",
                        Modifier.weight(1f),
                        MaterialTheme.colorScheme.error
                    )
                }
            }

            item { ChartCard(entries, maxV) }

            if (entries.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.home_empty_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { e ->
                    SwipeableEntryRow(
                        entry = e,
                        onClick = { onEditClick(e.id) },
                        onDeleteRequest = { pendingDelete = e }
                    )
                }
            }
        }
    }

    pendingDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(e); pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryRow(
    entry: PeakFlowEntry,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) { onDeleteRequest(); false } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    ) {
        EntryRow(entry, onClick)
    }
}

@Composable
private fun ChartCard(entries: List<PeakFlowEntry>, personalBest: Int?) {
    val pagerState = rememberPagerState(pageCount = { ChartPeriod.entries.size })
    val scope = rememberCoroutineScope()
    val labels = listOf(stringResource(R.string.home_chart_week),
                        stringResource(R.string.home_chart_month))

    Card(Modifier.fillMaxWidth()) {
        Column {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                labels.forEachIndexed { i, label ->
                    Tab(
                        selected = pagerState.currentPage == i,
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        text = { Text(label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(280.dp)
            ) { page ->
                val period = ChartPeriod.entries[page]
                val filtered = filterByPeriod(entries, period)
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.home_chart_empty_period),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Legend()
                        PeakFlowChart(filtered, personalBest, Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendDot(Color(0xFF2196F3), stringResource(R.string.home_legend_before))
        LegendDot(Color(0xFF4CAF50), stringResource(R.string.home_legend_after))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp)
                .then(Modifier)
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawCircle(color)
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, accent: Color) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(stringResource(R.string.unit_l_min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntryRow(e: PeakFlowEntry, onClick: () -> Unit) {
    val fmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(fmt.format(Date(e.timestamp)), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                val timing = if (e.timing == Timing.BEFORE)
                    stringResource(R.string.home_legend_before)
                    else stringResource(R.string.home_legend_after)
                val period = if (e.period == Period.MORNING)
                    stringResource(R.string.add_period_morning)
                    else stringResource(R.string.add_period_evening)
                Text("$period · $timing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${e.value}", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
