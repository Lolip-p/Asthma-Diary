package com.lolipp.asthmadiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeakFlowDao {
    @Query("SELECT * FROM peak_flow_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PeakFlowEntry>>

    @Query("SELECT MAX(value) FROM peak_flow_entries")
    fun getMax(): Flow<Int?>

    @Query("SELECT MIN(value) FROM peak_flow_entries")
    fun getMin(): Flow<Int?>

    @Insert
    suspend fun insert(entry: PeakFlowEntry): Long

    @Delete
    suspend fun delete(entry: PeakFlowEntry)
}
