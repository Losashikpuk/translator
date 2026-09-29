package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.HistoryRepository

class TransMutateApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
