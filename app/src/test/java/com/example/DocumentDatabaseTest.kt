package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DocumentDao
import com.example.data.model.GeneratedDocument
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DocumentDao
    private lateinit var repository: DocumentRepository
    private lateinit var testDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDir = File(context.cacheDir, "test_docs").apply { mkdirs() }
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.documentDao()
        repository = DocumentRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
        testDir.deleteRecursively()
    }

    @Test
    fun testInsertAndRetrieveDocument() = runBlocking {
        val doc = GeneratedDocument(
            title = "WeasyPrint Test Invoice",
            docType = "INVOICE",
            filePath = File(testDir, "test_inv.pdf").absolutePath,
            fileSizeBytes = 45200L,
            engineUsed = "weasyprint",
            durationMs = 120L,
            summary = "Invoice #INV-2026-001 for Acme Corp",
            sourceContent = "<html><body>Invoice</body></html>"
        )

        val insertedId = repository.insertDocument(doc)
        assertTrue("Inserted ID should be positive", insertedId > 0)

        val retrieved = repository.getDocumentById(insertedId).first()
        assertNotNull("Retrieved document should not be null", retrieved)
        assertEquals("WeasyPrint Test Invoice", retrieved?.title)
        assertEquals("INVOICE", retrieved?.docType)
        assertEquals("weasyprint", retrieved?.engineUsed)
        assertEquals(45200L, retrieved?.fileSizeBytes)
    }

    @Test
    fun testFilterDocumentsByType() = runBlocking {
        repository.insertDocument(
            GeneratedDocument(
                title = "Invoice #1",
                docType = "INVOICE",
                filePath = "/tmp/inv1.pdf",
                fileSizeBytes = 1000L,
                engineUsed = "weasyprint"
            )
        )
        repository.insertDocument(
            GeneratedDocument(
                title = "Report #1",
                docType = "REPORT",
                filePath = "/tmp/rep1.pdf",
                fileSizeBytes = 2000L,
                engineUsed = "reportlab"
            )
        )
        repository.insertDocument(
            GeneratedDocument(
                title = "Invoice #2",
                docType = "INVOICE",
                filePath = "/tmp/inv2.pdf",
                fileSizeBytes = 1500L,
                engineUsed = "fpdf2"
            )
        )

        val invoices = repository.getDocumentsByType("INVOICE").first()
        assertEquals("Should have 2 invoices", 2, invoices.size)

        val reports = repository.getDocumentsByType("REPORT").first()
        assertEquals("Should have 1 report", 1, reports.size)
        assertEquals("Report #1", reports[0].title)
    }

    @Test
    fun testDeleteDocument() = runBlocking {
        val testPdf = File(testDir, "delete_me.pdf").apply {
            writeText("dummy pdf content")
        }
        assertTrue(testPdf.exists())

        val doc = GeneratedDocument(
            title = "Temporary Doc",
            docType = "CUSTOM_HTML",
            filePath = testPdf.absolutePath,
            fileSizeBytes = testPdf.length(),
            engineUsed = "weasyprint"
        )

        val id = repository.insertDocument(doc)
        val inserted = repository.getDocumentById(id).first()!!

        repository.deleteDocument(inserted)

        val afterDelete = repository.getDocumentById(id).first()
        assertEquals("Document should be removed from database", null, afterDelete)
        assertTrue("Physical file should be deleted", !testPdf.exists())
    }

    @Test
    fun testClearAllDocuments() = runBlocking {
        repository.insertDocument(
            GeneratedDocument(title = "Doc 1", docType = "INVOICE", filePath = "/tmp/1.pdf", fileSizeBytes = 100, engineUsed = "weasyprint")
        )
        repository.insertDocument(
            GeneratedDocument(title = "Doc 2", docType = "REPORT", filePath = "/tmp/2.pdf", fileSizeBytes = 200, engineUsed = "weasyprint")
        )

        val allBefore = repository.allDocuments.first()
        assertEquals(2, allBefore.size)

        repository.clearAll()
        val allAfter = repository.allDocuments.first()
        assertEquals(0, allAfter.size)
    }
}
