package com.lolipp.asthmadiary.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lolipp.asthmadiary.data.AppDatabase
import com.lolipp.asthmadiary.data.PeakFlowEntry
import com.lolipp.asthmadiary.data.Period
import com.lolipp.asthmadiary.data.Timing
import kotlinx.coroutines.launch

class AddEntryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).peakFlowDao()

    fun load(id: Long, onLoaded: (PeakFlowEntry?) -> Unit) {
        viewModelScope.launch { onLoaded(dao.getById(id)) }
    }

    fun save(
        existingId: Long?,
        value: Int,
        timing: Timing,
        period: Period,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            if (existingId != null) {
                val old = dao.getById(existingId)
                if (old != null) dao.update(old.copy(value = value, timing = timing, period = period))
            } else {
                dao.insert(
                    PeakFlowEntry(
                        timestamp = System.currentTimeMillis(),
                        value = value, timing = timing, period = period
                    )
                )
            }
            onDone()
        }
    }
}
