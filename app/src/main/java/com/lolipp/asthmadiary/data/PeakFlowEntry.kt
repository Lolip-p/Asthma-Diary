package com.lolipp.asthmadiary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Timing { BEFORE, AFTER }
enum class Period { MORNING, EVENING }

@Entity(tableName = "peak_flow_entries")
data class PeakFlowEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val value: Int,
    val timing: Timing,
    val period: Period,
    val note: String = ""
)
