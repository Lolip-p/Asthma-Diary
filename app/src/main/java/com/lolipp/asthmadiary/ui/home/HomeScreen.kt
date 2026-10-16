package com.lolipp.asthmadiary.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.lolipp.asthmadiary.ui.chart.PeakFlowChart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChartPeriod(val label: String) {
    Week("Неделя"),
    Month("Месяц")
}

private fun filterByPeriod(entries: List<PeakFlowEntry>, period: ChartPeriod): List<PeakFlowEntry> {
    val now = System.currentTimeMillis()
    val cutoff = when (period) {
        ChartPeriod.Week -> now - 7L * 24 * 60 * 60 * 1000
        ChartPeriod.Month -> now - 30L * 24 * 60 * 60 * 1000
    }
    return entries.filter { it.timestamp >= cutoff }
}

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
                ChartCard(entries = entries, personalBest = maxV)
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
                items(entries, key = { it.id }) { e -> EntryRow(e) }
            }
        }
    }
}

@Composable
private fun ChartCard(entries: List<PeakFlowEntry>, personalBest: Int?) {
    val pagerState = rememberPagerState(pageCount = { ChartPeriod.entries.size })
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                ChartPeriod.entries.forEachIndexed { i, p ->
                    Tab(
                        selected = pagerState.currentPage == i,
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        text = { Text(p.label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(260.dp)
            ) { page ->
                val period = ChartPeriod.entries[page]
                val filtered = filterByPeriod(entries, period)
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Нет данных за период",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    PeakFlowChart(
                        entries = filtered,
                        personalBest = personalBest,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
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
