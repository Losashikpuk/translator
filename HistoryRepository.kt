package com.example.data.repository

import com.example.data.db.HistoryDao
import com.example.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = dao.getAllHistory()
    val favoriteHistory: Flow<List<HistoryEntity>> = dao.getFavorites()

    suspend fun saveTransformation(
        originalText: String,
        finalText: String,
        sourceLang: String,
        targetLang: String,
        hopsCount: Int,
        intensity: String,
        stepsJson: String
    ): Long {
        val entity = HistoryEntity(
            originalText = originalText,
            finalText = finalText,
            sourceLang = sourceLang,
            targetLang = targetLang,
            hopsCount = hopsCount,
            intensity = intensity,
            intermediateStepsJson = stepsJson
        )
        return dao.insertHistory(entity)
    }

    suspend fun toggleFavorite(id: Long, currentFav: Boolean) {
        dao.updateFavorite(id, !currentFav)
    }

    suspend fun delete(id: Long) {
        dao.deleteHistory(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
