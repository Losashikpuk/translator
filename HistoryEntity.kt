package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transformation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val finalText: String,
    val sourceLang: String,
    val targetLang: String,
    val hopsCount: Int,
    val intensity: String,
    val intermediateStepsJson: String, // JSON string of steps
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
