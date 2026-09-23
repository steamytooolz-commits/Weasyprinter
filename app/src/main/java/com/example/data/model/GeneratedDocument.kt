package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_documents")
data class GeneratedDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val docType: String, // INVOICE, REPORT, CERTIFICATE, CUSTOM_HTML, PYTHON_SCRIPT
    val filePath: String,
    val fileSizeBytes: Long,
    val engineUsed: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val summary: String = "",
    val sourceContent: String = ""
)
