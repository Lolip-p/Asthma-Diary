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

    private const val MIN_Y = 50f
    private const val MAX_Y = 800f
    private const val STEP_Y = 25
    private const val PERIOD_DAYS = 30

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
        // A4 landscape: 842 x 595 pt
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        val paintText = Paint().apply { color = Color.BLACK; textSize = 12f; isAntiAlias = true }
        val paintTitle = Paint().apply {
            color = Color.BLACK; textSize = 20f; isFakeBoldText = true; isAntiAlias = true
        }
        val paintMuted = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }

        var y = 40f
        c.drawText(context.getString(R.string.pdf_title), 40f, y, paintTitle); y += 24f

        val now = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        c.drawText(context.getString(R.string.pdf_generated, now), 40f, y, paintMuted); y += 18f

        c.drawText(
            context.getString(
                R.string.pdf_stats,
                personalBest?.toString() ?: "—",
                minimum?.toString() ?: "—",
                entries.size
            ),
            40f, y, paintText
        ); y += 26f

        // === Chart (wide) ===
        val chartLeft = 70f
        val chartRight = 820f
        val chartTop = y + 6f
        val chartBottom = 555f
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        fun yOf(v: Float): Float {
            val clamped = v.coerceIn(MIN_Y, MAX_Y)
            return chartBottom - ((clamped - MIN_Y) / (MAX_Y - MIN_Y)) * chartH
        }

        // grid Y each 25
        val gridPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.4f }
        var v = MIN_Y.toInt()
        while (v <= MAX_Y.toInt()) {
            val yy = yOf(v.toFloat())
            c.drawLine(chartLeft, yy, chartRight, yy, gridPaint)
            v += STEP_Y
        }
        // labels each 100
        var vLabel = 100
        while (vLabel <= MAX_Y.toInt()) {
            val yy = yOf(vLabel.toFloat())
            c.drawText(vLabel.toString(), 40f, yy + 3f, paintMuted)
            vLabel += 100
        }
        c.drawText("50", 40f, yOf(50f) + 3f, paintMuted)
        c.drawText("800", 40f, yOf(800f) + 3f, paintMuted)

        // axis
        val axisPaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
        c.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
        c.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        // === X axis: last 30 days (fixed) ===
        val nowMs = System.currentTimeMillis()
        val tEnd = nowMs
        val tStart = nowMs - PERIOD_DAYS.toLong() * 24L * 60 * 60 * 1000
        fun xOf(t: Long): Float =
            chartLeft + ((t - tStart).toFloat() / (tEnd - tStart).toFloat()) * chartW

        val dateFmt = SimpleDateFormat("dd.MM", Locale.getDefault())
        var tLabel = tStart
        val labelStep = 5L * 24 * 60 * 60 * 1000
        while (tLabel <= tEnd) {
            c.drawText(dateFmt.format(Date(tLabel)), xOf(tLabel) - 12f, chartBottom + 16f, paintMuted)
            tLabel += labelStep
        }

        val sorted = entries.sortedBy { it.timestamp }
        if (sorted.size >= 1) {
            val beforePaint = Paint().apply {
                color = Color.rgb(33, 150, 243); strokeWidth = 2.5f; isAntiAlias = true
            }
            val afterPaint = Paint().apply {
                color = Color.rgb(76, 175, 80); strokeWidth = 2.5f; isAntiAlias = true
            }
            val beforeDot = Paint().apply { color = Color.rgb(33, 150, 243); isAntiAlias = true }
            val afterDot = Paint().apply { color = Color.rgb(76, 175, 80); isAntiAlias = true }

            fun drawSeries(list: List<PeakFlowEntry>, line: Paint, dot: Paint) {
                for (i in 0 until list.size - 1) {
                    c.drawLine(
                        xOf(list[i].timestamp), yOf(list[i].value.toFloat()),
                        xOf(list[i + 1].timestamp), yOf(list[i + 1].value.toFloat()),
                        line
                    )
                }
                list.forEach {
                    c.drawCircle(xOf(it.timestamp), yOf(it.value.toFloat()), 4f, dot)
                }
            }
            drawSeries(sorted.filter { it.timing == Timing.BEFORE }, beforePaint, beforeDot)
            drawSeries(sorted.filter { it.timing == Timing.AFTER }, afterPaint, afterDot)

            // legend top-right
            val lgY = chartTop - 2f
            var lgX = chartRight - 200f
            c.drawCircle(lgX, lgY, 5f, beforeDot)
            c.drawText("Before", lgX + 10f, lgY + 4f, paintMuted)
            lgX += 100f
            c.drawCircle(lgX, lgY, 5f, afterDot)
            c.drawText("After", lgX + 10f, lgY + 4f, paintMuted)
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
