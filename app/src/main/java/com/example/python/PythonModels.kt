package com.example.python

data class GenerationResult(
    val success: Boolean,
    val engineUsed: String,
    val outputPath: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val weasyprintAttempted: Boolean = false,
    val weasyprintError: String? = null,
    val error: String? = null,
    val title: String = "",
    val docType: String = "",
    val summary: String = "",
    val sourceContent: String = ""
)

data class ExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val pdfCreated: Boolean,
    val outputPdfPath: String?,
    val fileSizeBytes: Long
)

data class PackageDiagnostic(
    val available: Boolean,
    val version: String?,
    val error: String?
)

data class EngineDiagnostics(
    val pythonVersion: String,
    val platform: String,
    val executable: String,
    val weasyprint: PackageDiagnostic,
    val reportlab: PackageDiagnostic,
    val fpdf2: PackageDiagnostic,
    val jinja2: PackageDiagnostic,
    val markdown: PackageDiagnostic
)

data class InvoiceItemData(
    val desc: String,
    val qty: Int,
    val rate: Double,
    val amount: Double = qty * rate
)

data class InvoiceFormData(
    val invoiceNo: String = "INV-2026-001",
    val date: String = "2026-09-23",
    val dueDate: String = "2026-10-23",
    val senderName: String = "TechPulse Innovations LLC",
    val senderAddress: String = "100 Innovation Way, Suite 400\nSan Francisco, CA 94105",
    val senderEmail: String = "billing@techpulse.io",
    val clientName: String = "Acme Global Solutions",
    val clientAddress: String = "456 Enterprise Blvd\nAustin, TX 78701",
    val clientEmail: String = "finance@acme.corp",
    val items: List<InvoiceItemData> = listOf(
        InvoiceItemData("Full-Stack Android Python Architecture", 1, 2500.0),
        InvoiceItemData("WeasyPrint & Chaquopy Engine Pipeline", 1, 1800.0),
        InvoiceItemData("Automated PDF Reporting Module", 2, 650.0)
    ),
    val taxRate: Double = 8.25,
    val discount: Double = 0.0,
    val currency: String = "$",
    val notes: String = "Thank you for your business! Payment is due within 30 days via ACH or Wire Transfer."
)

data class ReportSectionData(
    val heading: String,
    val body: String
)

data class ReportFormData(
    val title: String = "Quarterly Operations & Performance Report",
    val subtitle: String = "Q3 2026 Executive Summary & Strategic Insights",
    val author: String = "Data Analytics & Engineering Group",
    val date: String = "September 23, 2026",
    val sections: List<ReportSectionData> = listOf(
        ReportSectionData(
            "1. Executive Summary",
            "This report outlines operational benchmarks, Python runtime throughput, and automated document generation efficiency on mobile devices. Through Chaquopy's embedded CPython runtime, on-device generation latency has decreased by 42%."
        ),
        ReportSectionData(
            "2. Performance Milestones",
            "System stability achieved 99.98% uptime across all compilation targets. Native ABI bindings for arm64-v8a and x86_64 allow direct binary execution without network roundtrips."
        ),
        ReportSectionData(
            "3. Future Architecture Roadmap",
            "Planned enhancements include local font caching, multi-page asynchronous streaming, and accelerated layout computation with pre-compiled CSS stylesheets."
        )
    )
)

data class CertificateFormData(
    val recipientName: String = "Alex M. Henderson",
    val title: String = "CERTIFICATE OF EXCELLENCE",
    val subtitle: String = "For outstanding proficiency in Embedded Mobile Python Systems",
    val courseOrAchievement: String = "Advanced Android Architecture & Chaquopy Integration",
    val date: String = "September 23, 2026",
    val issuer: String = "Python Software Foundation & AI Studio Academy",
    val signatory: String = "Dr. Elena Rostova, Dean of Engineering"
)
