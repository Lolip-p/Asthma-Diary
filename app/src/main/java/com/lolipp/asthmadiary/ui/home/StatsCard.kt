package com.lolipp.asthmadiary.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolipp.asthmadiary.R

@Composable
fun StatsCard(stats: Stats) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            StatRow(
                label = stringResource(R.string.stats_week_avg),
                value = stats.weekAvg?.toString() ?: "—",
                suffix = stringResource(R.string.unit_l_min)
            )
            StatRow(
                label = stringResource(R.string.stats_month_avg),
                value = stats.monthAvg?.toString() ?: "—",
                suffix = stringResource(R.string.unit_l_min)
            )

            val (trendLabel, trendColor) = when (stats.trend) {
                Trend.UP -> stringResource(R.string.stats_trend_up) to MaterialTheme.colorScheme.primary
                Trend.DOWN -> stringResource(R.string.stats_trend_down) to MaterialTheme.colorScheme.error
                Trend.FLAT -> stringResource(R.string.stats_trend_flat) to MaterialTheme.colorScheme.onSurface
            }
            StatRow(
                label = stringResource(R.string.stats_trend_label),
                value = trendLabel,
                suffix = null,
                valueColor = trendColor
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    suffix: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        if (suffix != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
