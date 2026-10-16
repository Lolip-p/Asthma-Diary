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
import com.lolipp.asthmadiary.data.Timing
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

    val beforeColor = Color(0xFF2196F3) // синий — до
    val afterColor = Color(0xFF4CAF50)  // зелёный — после
    val zoneGreen = Color(0xFF4CAF50)
    val zoneYellow = Color(0xFFFFC107)
    val zoneRed = Color(0xFFE53935)

    Canvas(modifier = modifier) {
        if (entries.isEmpty()) return@Canvas

        val leftPad = 44.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 20.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val sortedAsc = entries.sortedBy { it.timestamp }
        val maxEntry = sortedAsc.maxOf { it.value }
        val pb = personalBest ?: 0
        val rawMax = max(maxEntry, pb)
        val maxY = (((rawMax + 99) / 100) * 100).coerceAtLeast(400)

        fun yOf(v: Float) = topPad + chartH - (v / maxY) * chartH

        // зоны
        if (pb > 0) {
            val greenFrom = pb * 0.8f
            val yellowFrom = pb * 0.5f
            drawRect(
                color = zoneGreen.copy(alpha = 0.10f),
                topLeft = Offset(leftPad, topPad),
                size = Size(chartW, (yOf(greenFrom) - topPad).coerceAtLeast(0f))
            )
            drawRect(
                color = zoneYellow.copy(alpha = 0.14f),
                topLeft = Offset(leftPad, yOf(greenFrom)),
                size = Size(chartW, (yOf(yellowFrom) - yOf(greenFrom)).coerceAtLeast(0f))
            )
            drawRect(
                color = zoneRed.copy(alpha = 0.14f),
                topLeft = Offset(leftPad, yOf(yellowFrom)),
                size = Size(chartW, (topPad + chartH - yOf(yellowFrom)).coerceAtLeast(0f))
            )
        }

        // сетка Y
        var yVal = 0
        while (yVal <= maxY) {
            val y = yOf(yVal.toFloat())
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + chartW, y), 1.dp.toPx())
            val layout = textMeasurer.measure(
                text = yVal.toString(),
                style = TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                layout,
                topLeft = Offset(leftPad - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f)
            )
            yVal += 100
        }
        drawLine(gridColor, Offset(leftPad, topPad), Offset(leftPad, topPad + chartH), 1.dp.toPx())
        drawLine(gridColor, Offset(leftPad, topPad + chartH), Offset(leftPad + chartW, topPad + chartH), 1.dp.toPx())

        // X
        val tStart = sortedAsc.first().timestamp
        val tEnd = sortedAsc.last().timestamp
        val tRange = (tEnd - tStart).coerceAtLeast(1L)
        fun xOf(t: Long) = leftPad + ((t - tStart).toFloat() / tRange) * chartW

        val dateFmt = SimpleDateFormat("d MMM", Locale.getDefault())
        listOf(tStart, tStart + tRange / 2, tEnd).distinct().forEach { t ->
            val layout = textMeasurer.measure(
                text = dateFmt.format(Date(t)),
                style = TextStyle(color = labelColor, fontSize = 10.sp)
            )
            drawText(
                layout,
                topLeft = Offset(
                    (xOf(t) - layout.size.width / 2f)
                        .coerceIn(leftPad, leftPad + chartW - layout.size.width),
                    topPad + chartH + 6.dp.toPx()
                )
            )
        }

        // серии
        val beforeList = sortedAsc.filter { it.timing == Timing.BEFORE }
        val afterList = sortedAsc.filter { it.timing == Timing.AFTER }

        fun drawSeries(list: List<PeakFlowEntry>, color: Color) {
            if (list.size >= 2) {
                val path = Path()
                list.forEachIndexed { i, e ->
                    val x = xOf(e.timestamp); val y = yOf(e.value.toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
            list.forEach { e ->
                val x = xOf(e.timestamp); val y = yOf(e.value.toFloat())
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = Offset(x, y))
            }
        }
        drawSeries(beforeList, beforeColor)
        drawSeries(afterList, afterColor)
    }
}
