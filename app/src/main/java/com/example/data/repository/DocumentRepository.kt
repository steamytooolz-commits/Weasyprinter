package com.example.data.repository

import com.example.data.db.DocumentDao
import com.example.data.model.GeneratedDocument
import kotlinx.coroutines.flow.Flow
import java.io.File

class DocumentRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<GeneratedDocument>> = dao.getAllDocuments()

    fun getDocumentById(id: Long): Flow<GeneratedDocument?> = dao.getDocumentById(id)

    fun getDocumentsByType(type: String): Flow<List<GeneratedDocument>> = dao.getDocumentsByType(type)

    suspend fun insertDocument(document: GeneratedDocument): Long = dao.insertDocument(document)

    suspend fun deleteDocument(document: GeneratedDocument) {
        // Also remove the physical PDF file if present
        try {
            val file = File(document.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.deleteDocument(document)
    }

    suspend fun deleteDocumentById(id: Long, filePath: String?) {
        if (!filePath.isNullOrBlank()) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
        dao.deleteDocumentById(id)
    }

    suspend fun clearAll() = dao.clearAll()
}
