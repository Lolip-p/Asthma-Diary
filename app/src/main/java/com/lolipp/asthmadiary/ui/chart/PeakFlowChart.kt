package com.lolipp.asthmadiary.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolipp.asthmadiary.data.PeakFlowEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun PeakFlowChart(
    entries: List<PeakFlowEntry>,
    personalBest: Int?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val zoneGreen = Color(0xFF4CAF50)
    val zoneYellow = Color(0xFFFFC107)
    val zoneRed = Color(0xFFE53935)

    Canvas(modifier = modifier) {
        if (entries.isEmpty()) return@Canvas

        val leftPad = 44.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val sortedAsc = entries.sortedBy { it.timestamp }
        val maxEntry = sortedAsc.maxOf { it.value }
        val pb = personalBest ?: 0
        val rawMax = max(maxEntry, pb)
        val maxY = (((rawMax + 99) / 100) * 100).coerceAtLeast(400)

        fun yOf(v: Float) = topPad + chartH - (v / maxY) * chartH

        // Цветовые зоны по личному рекорду (asthma action plan)
        if (pb > 0) {
            val greenFrom = pb * 0.8f
            val yellowFrom = pb * 0.5f

            drawRect(
                color = zoneGreen.copy(alpha = 0.15f),
                topLeft = Offset(leftPad, topPad),
                size = Size(chartW, (yOf(greenFrom) - topPad).coerceAtLeast(0f))
            )
            drawRect(
                color = zoneYellow.copy(alpha = 0.18f),
                topLeft = Offset(leftPad, yOf(greenFrom)),
                size = Size(chartW, (yOf(yellowFrom) - yOf(greenFrom)).coerceAtLeast(0f))
            )
            drawRect(
                color = zoneRed.copy(alpha = 0.18f),
                topLeft = Offset(leftPad, yOf(yellowFrom)),
                size = Size(chartW, (topPad + chartH - yOf(yellowFrom)).coerceAtLeast(0f))
            )
        }

        // Сетка + подписи Y (каждые 100)
        var yVal = 0
        while (yVal <= maxY) {
            val y = yOf(yVal.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.dp.toPx()
            )
            val layout = textMeasurer.measure(
                text = yVal.toString(),
                style = TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = leftPad - layout.size.width - 6.dp.toPx(),
                    y = y - layout.size.height / 2f
                )
            )
            yVal += 100
        }

        // Границы графика
        drawLine(gridColor, Offset(leftPad, topPad), Offset(leftPad, topPad + chartH), 1.dp.toPx())
        drawLine(gridColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), 1.dp.toPx())

        // X: время
        val tStart = sortedAsc.first().timestamp
        val tEnd = sortedAsc.last().timestamp
        val tRange = (tEnd - tStart).coerceAtLeast(1L)
        fun xOf(t: Long) = leftPad + ((t - tStart).toFloat() / tRange) * chartW

        val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())
        val labelTimes = listOf(tStart, tStart + tRange / 2, tEnd).distinct()
        labelTimes.forEach { t ->
            val layout = textMeasurer.measure(
                text = dateFmt.format(Date(t)),
                style = TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = (xOf(t) - layout.size.width / 2f)
                        .coerceIn(leftPad, leftPad + chartW - layout.size.width),
                    y = topPad + chartH + 6.dp.toPx()
                )
            )
        }

        // Линия
        if (sortedAsc.size >= 2) {
            val path = Path()
            sortedAsc.forEachIndexed { i, e ->
                val x = xOf(e.timestamp)
                val y = yOf(e.value.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
        }

        // Точки
        sortedAsc.forEach { e ->
            val x = xOf(e.timestamp)
            val y = yOf(e.value.toFloat())
            drawCircle(color = pointColor, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = Offset(x, y))
        }
    }
}
