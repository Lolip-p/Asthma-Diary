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

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).peakFlowDao()

    val entries: StateFlow<List<PeakFlowEntry>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val maxValue: StateFlow<Int?> = dao.getMax()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val minValue: StateFlow<Int?> = dao.getMin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(entry: PeakFlowEntry) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
