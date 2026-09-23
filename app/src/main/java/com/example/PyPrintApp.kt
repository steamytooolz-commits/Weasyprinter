package com.example

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.data.db.AppDatabase
import com.example.data.repository.DocumentRepository

class PyPrintApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: DocumentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Chaquopy Python Runtime (skipped in JVM unit tests)
        if (!isUnitTest()) {
            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(this))
                    Log.d("PyPrintApp", "Chaquopy Python runtime started successfully.")
                }
            } catch (t: Throwable) {
                Log.e("PyPrintApp", "Failed to start Python runtime", t)
            }
        }

        // 2. Initialize Room Database
        database = AppDatabase.getDatabase(this)
        repository = DocumentRepository(database.documentDao())
    }

    private fun isUnitTest(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric")
            true
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        lateinit var instance: PyPrintApp
            private set
    }
}
