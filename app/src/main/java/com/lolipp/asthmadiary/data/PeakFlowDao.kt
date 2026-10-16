package com.lolipp.asthmadiary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeakFlowDao {
    @Query("SELECT * FROM peak_flow_entries ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PeakFlowEntry>>

    @Query("SELECT * FROM peak_flow_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PeakFlowEntry?

    @Query("SELECT MAX(value) FROM peak_flow_entries")
    fun getMax(): Flow<Int?>

    @Query("SELECT MIN(value) FROM peak_flow_entries")
    fun getMin(): Flow<Int?>

    @Insert
    suspend fun insert(entry: PeakFlowEntry): Long

    @Update
    suspend fun update(entry: PeakFlowEntry)

    @Delete
    suspend fun delete(entry: PeakFlowEntry)
}
