package com.example

import com.example.python.CertificateFormData
import com.example.python.EngineDiagnostics
import com.example.python.ExecutionResult
import com.example.python.GenerationResult
import com.example.python.InvoiceFormData
import com.example.python.InvoiceItemData
import com.example.python.PackageDiagnostic
import com.example.python.ReportFormData
import com.example.python.ReportSectionData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentModelsTest {

    @Test
    fun testInvoiceSubtotalAndTaxCalculation() {
        val items = listOf(
            InvoiceItemData("Python WeasyPrint Mobile Engine", qty = 2, rate = 500.0),
            InvoiceItemData("Android UI Design & Compose Integration", qty = 1, rate = 1200.0),
            InvoiceItemData("Offline PDF Document Library", qty = 3, rate = 150.0)
        )

        // Subtotal = (2*500) + (1*1200) + (3*150) = 1000 + 1200 + 450 = 2650.0
        val subtotal = items.sumOf { it.amount }
        assertEquals(2650.0, subtotal, 0.001)

        val taxRate = 8.5
        val taxAmount = subtotal * (taxRate / 100.0)
        assertEquals(225.25, taxAmount, 0.001)

        val discount = 150.0
        val total = subtotal + taxAmount - discount
        assertEquals(2725.25, total, 0.001)

        val invoiceData = InvoiceFormData(
            invoiceNo = "INV-TEST-99",
            items = items,
            taxRate = taxRate,
            discount = discount
        )

        assertEquals("INV-TEST-99", invoiceData.invoiceNo)
        assertEquals(3, invoiceData.items.size)
        assertEquals(1000.0, invoiceData.items[0].amount, 0.001)
    }

    @Test
    fun testReportFormDataStructure() {
        val report = ReportFormData(
            title = "WeasyPrint Performance Benchmarks",
            subtitle = "Evaluation of HTML-to-PDF rendering on Android",
            author = "Engineering Team",
            sections = listOf(
                ReportSectionData("Preamble", "Testing WeasyPrint CFFI and Cairo bindings on Android."),
                ReportSectionData("Throughput", "Rendered 10-page document in 840ms.")
            )
        )

        assertEquals("WeasyPrint Performance Benchmarks", report.title)
        assertEquals(2, report.sections.size)
        assertEquals("Preamble", report.sections[0].heading)
        assertTrue(report.sections[1].body.contains("840ms"))
    }

    @Test
    fun testCertificateFormDataStructure() {
        val cert = CertificateFormData(
            recipientName = "Ada Lovelace",
            title = "CERTIFICATE OF EXCELLENCE",
            courseOrAchievement = "Pioneering Computational Algorithms"
        )

        assertEquals("Ada Lovelace", cert.recipientName)
        assertEquals("CERTIFICATE OF EXCELLENCE", cert.title)
        assertNotNull(cert.issuer)
    }

    @Test
    fun testGenerationResultDataClass() {
        val successResult = GenerationResult(
            success = true,
            engineUsed = "weasyprint",
            outputPath = "/data/user/0/com.example/files/documents/invoice_001.pdf",
            fileSizeBytes = 34560L,
            durationMs = 210L,
            weasyprintAttempted = true,
            title = "Invoice #001",
            docType = "INVOICE"
        )

        assertTrue(successResult.success)
        assertEquals("weasyprint", successResult.engineUsed)
        assertEquals(34560L, successResult.fileSizeBytes)
        assertTrue(successResult.weasyprintAttempted)

        val fallbackResult = GenerationResult(
            success = true,
            engineUsed = "reportlab",
            outputPath = "/data/user/0/com.example/files/documents/invoice_fallback.pdf",
            fileSizeBytes = 28900L,
            durationMs = 95L,
            weasyprintAttempted = true,
            weasyprintError = "WeasyPrint native library libpango not found",
            title = "Invoice Fallback",
            docType = "INVOICE"
        )

        assertTrue(fallbackResult.success)
        assertEquals("reportlab", fallbackResult.engineUsed)
        assertNotNull(fallbackResult.weasyprintError)
    }

    @Test
    fun testExecutionResultDataClass() {
        val exec = ExecutionResult(
            success = true,
            stdout = "Starting WeasyPrint compilation...\nPDF generated successfully.\n",
            stderr = "",
            durationMs = 150L,
            pdfCreated = true,
            outputPdfPath = "/tmp/out.pdf",
            fileSizeBytes = 12400L
        )

        assertTrue(exec.success)
        assertTrue(exec.pdfCreated)
        assertTrue(exec.stdout.contains("WeasyPrint"))
        assertEquals("", exec.stderr)
    }

    @Test
    fun testEngineDiagnostics() {
        val diagnostics = EngineDiagnostics(
            pythonVersion = "3.11.8",
            platform = "Linux-arm64-v8a",
            executable = "/data/user/0/com.example/files/chaquopy/bin/python3",
            weasyprint = PackageDiagnostic(available = true, version = "60.2", error = null),
            reportlab = PackageDiagnostic(available = true, version = "4.2.0", error = null),
            fpdf2 = PackageDiagnostic(available = true, version = "2.7.9", error = null),
            jinja2 = PackageDiagnostic(available = true, version = "3.1.3", error = null),
            markdown = PackageDiagnostic(available = true, version = "3.6", error = null)
        )

        assertEquals("3.11.8", diagnostics.pythonVersion)
        assertTrue(diagnostics.weasyprint.available)
        assertEquals("60.2", diagnostics.weasyprint.version)
        assertTrue(diagnostics.jinja2.available)
    }
}
