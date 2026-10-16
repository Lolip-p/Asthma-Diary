package com.lolipp.asthmadiary.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.lolipp.asthmadiary.R
import com.lolipp.asthmadiary.data.PeakFlowEntry
import com.lolipp.asthmadiary.data.Timing
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfExporter {

    fun buildAndShare(
        context: Context,
        entries: List<PeakFlowEntry>,
        personalBest: Int?,
        minimum: Int?
    ) {
        val file = generate(context, entries, personalBest, minimum)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.pdf_share_title))
        )
    }

    private fun generate(
        context: Context,
        entries: List<PeakFlowEntry>,
        personalBest: Int?,
        minimum: Int?
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        val paintText = Paint().apply { color = Color.BLACK; textSize = 12f; isAntiAlias = true }
        val paintTitle = Paint().apply { color = Color.BLACK; textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        val paintMuted = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }

        var y = 50f
        c.drawText(context.getString(R.string.pdf_title), 40f, y, paintTitle); y += 24f

        val now = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        c.drawText(context.getString(R.string.pdf_generated, now), 40f, y, paintMuted); y += 20f

        c.drawText(
            context.getString(
                R.string.pdf_stats,
                personalBest?.toString() ?: "—",
                minimum?.toString() ?: "—",
                entries.size
            ),
            40f, y, paintText
        ); y += 24f

        // Chart
        val chartLeft = 60f
        val chartRight = 555f
        val chartTop = y + 10f
        val chartBottom = chartTop + 220f
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val sorted = entries.sortedBy { it.timestamp }
        if (sorted.size >= 2) {
            val rawMax = max(sorted.maxOf { it.value }, personalBest ?: 0)
            val maxY = (((rawMax + 99) / 100) * 100).coerceAtLeast(400)
            val tStart = sorted.first().timestamp
            val tEnd = sorted.last().timestamp
            val tRange = (tEnd - tStart).coerceAtLeast(1L)

            fun yOf(v: Float) = chartBottom - (v / maxY) * chartH
            fun xOf(t: Long) = chartLeft + ((t - tStart).toFloat() / tRange) * chartW

            // grid
            val gridPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
            var v = 0
            while (v <= maxY) {
                c.drawLine(chartLeft, yOf(v.toFloat()), chartRight, yOf(v.toFloat()), gridPaint)
                c.drawText(v.toString(), 30f, yOf(v.toFloat()) + 3f, paintMuted)
                v += 100
            }
            c.drawLine(chartLeft, chartTop, chartLeft, chartBottom, gridPaint)
            c.drawLine(chartLeft, chartBottom, chartRight, chartBottom, gridPaint)

            // two lines
            val beforePaint = Paint().apply {
                color = Color.rgb(33, 150, 243); strokeWidth = 2f; isAntiAlias = true
            }
            val afterPaint = Paint().apply {
                color = Color.rgb(76, 175, 80); strokeWidth = 2f; isAntiAlias = true
            }
            val beforePoint = Paint().apply { color = Color.rgb(33, 150, 243); isAntiAlias = true }
            val afterPoint = Paint().apply { color = Color.rgb(76, 175, 80); isAntiAlias = true }

            val beforeList = sorted.filter { it.timing == Timing.BEFORE }
            val afterList = sorted.filter { it.timing == Timing.AFTER }

            fun drawSeries(list: List<PeakFlowEntry>, paint: Paint, dot: Paint) {
                for (i in 0 until list.size - 1) {
                    c.drawLine(
                        xOf(list[i].timestamp), yOf(list[i].value.toFloat()),
                        xOf(list[i + 1].timestamp), yOf(list[i + 1].value.toFloat()),
                        paint
                    )
                }
                list.forEach {
                    c.drawCircle(xOf(it.timestamp), yOf(it.value.toFloat()), 4f, dot)
                }
            }
            drawSeries(beforeList, beforePaint, beforePoint)
            drawSeries(afterList, afterPaint, afterPoint)

            // legend
            var lx = chartLeft
            val ly = chartTop - 4f
            c.drawCircle(lx, ly, 4f, beforePoint)
            c.drawText("Before", lx + 8f, ly + 4f, paintMuted)
            lx += 70f
            c.drawCircle(lx, ly, 4f, afterPoint)
            c.drawText("After", lx + 8f, ly + 4f, paintMuted)
        } else {
            c.drawText("Not enough data for chart", chartLeft, chartTop + 40f, paintMuted)
        }

        y = chartBottom + 30f

        // Recent entries table
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        c.drawText("Recent entries:", 40f, y, paintText); y += 18f
        val recent = sorted.takeLast(20).reversed()
        recent.forEach { e ->
            if (y > 800f) return@forEach
            val line = "${fmt.format(Date(e.timestamp))}   ${e.value} L/min   ${if (e.timing == Timing.BEFORE) "before" else "after"}"
            c.drawText(line, 40f, y, paintText)
            y += 16f
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "peakflow_$stamp.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
