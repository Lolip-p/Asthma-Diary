package com.lolipp.asthmadiary.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lolipp.asthmadiary.data.AppDatabase
import com.lolipp.asthmadiary.data.PeakFlowEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Stats(
    val weekAvg: Int? = null,
    val monthAvg: Int? = null,
    val trend: Trend = Trend.FLAT,
)

enum class Trend { UP, DOWN, FLAT }

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).peakFlowDao()

    val entries: StateFlow<List<PeakFlowEntry>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val maxValue: StateFlow<Int?> = dao.getMax()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val minValue: StateFlow<Int?> = dao.getMin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun computeStats(list: List<PeakFlowEntry>): Stats {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val weekCut = now - 7 * day
        val prevWeekCut = now - 14 * day
        val monthCut = now - 30 * day

        val weekList = list.filter { it.timestamp >= weekCut }
        val prevWeekList = list.filter { it.timestamp in prevWeekCut until weekCut }
        val monthList = list.filter { it.timestamp >= monthCut }

        val weekAvg = if (weekList.isEmpty()) null else weekList.map { it.value }.average().toInt()
        val monthAvg = if (monthList.isEmpty()) null else monthList.map { it.value }.average().toInt()

        val trend = run {
            if (weekList.isEmpty() || prevWeekList.isEmpty()) return@run Trend.FLAT
            val a = weekList.map { it.value }.average()
            val b = prevWeekList.map { it.value }.average()
            val diff = a - b
            when {
                diff > 15 -> Trend.UP
                diff < -15 -> Trend.DOWN
                else -> Trend.FLAT
            }
        }

        return Stats(weekAvg, monthAvg, trend)
    }

    fun delete(entry: PeakFlowEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
