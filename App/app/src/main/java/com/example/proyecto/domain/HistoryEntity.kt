package com.example.proyecto.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val confidence: Float,
    val timestamp: Long
)
