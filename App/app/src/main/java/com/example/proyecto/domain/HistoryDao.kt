package com.example.proyecto.domain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(item: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("DELETE FROM history")
    suspend fun clear()
}
