package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GeneratedDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM generated_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<GeneratedDocument>>

    @Query("SELECT * FROM generated_documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): Flow<GeneratedDocument?>

    @Query("SELECT * FROM generated_documents WHERE docType = :docType ORDER BY createdAt DESC")
    fun getDocumentsByType(docType: String): Flow<List<GeneratedDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: GeneratedDocument): Long

    @Delete
    suspend fun deleteDocument(document: GeneratedDocument)

    @Query("DELETE FROM generated_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM generated_documents")
    suspend fun clearAll()
}
