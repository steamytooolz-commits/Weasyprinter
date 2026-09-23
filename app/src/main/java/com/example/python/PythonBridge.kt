package com.example.python

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PythonBridge {

    private const val TAG = "PythonBridge"

    private fun getModule(): com.chaquo.python.PyObject? {
        return try {
            if (!Python.isStarted()) {
                val app = com.example.PyPrintApp.instance
                Python.start(com.chaquo.python.android.AndroidPlatform(app))
            }
            Python.getInstance().getModule("pdf_engine")
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to get Python module pdf_engine", e)
            null
        }
    }

    suspend fun getEngineDiagnostics(): EngineDiagnostics = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val rawJson = module.callAttr("get_engine_diagnostics").toString()
            val root = JSONObject(rawJson)

            val pyVer = root.optString("python_version", "Unknown")
            val platform = root.optString("platform", "Unknown")
            val exec = root.optString("executable", "Unknown")

            val pkgs = root.optJSONObject("packages") ?: JSONObject()

            fun parsePkg(name: String): PackageDiagnostic {
                val obj = pkgs.optJSONObject(name)
                return if (obj != null) {
                    PackageDiagnostic(
                        available = obj.optBoolean("available", false),
                        version = if (obj.has("version") && !obj.isNull("version")) obj.getString("version") else null,
                        error = if (obj.has("error") && !obj.isNull("error")) obj.getString("error") else null
                    )
                } else {
                    PackageDiagnostic(false, null, "Not found")
                }
            }

            EngineDiagnostics(
                pythonVersion = pyVer,
                platform = platform,
                executable = exec,
                weasyprint = parsePkg("weasyprint"),
                reportlab = parsePkg("reportlab"),
                fpdf2 = parsePkg("fpdf2"),
                jinja2 = parsePkg("jinja2"),
                markdown = parsePkg("markdown")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching diagnostics", e)
            EngineDiagnostics(
                pythonVersion = "Error: ${e.message}",
                platform = "Android",
                executable = "",
                weasyprint = PackageDiagnostic(false, null, e.message),
                reportlab = PackageDiagnostic(false, null, e.message),
                fpdf2 = PackageDiagnostic(false, null, e.message),
                jinja2 = PackageDiagnostic(false, null, e.message),
                markdown = PackageDiagnostic(false, null, e.message)
            )
        }
    }

    suspend fun generateInvoice(
        context: Context,
        data: InvoiceFormData
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val filename = "Invoice_${data.invoiceNo.replace(Regex("[^a-zA-Z0-9_]"), "_")}_${System.currentTimeMillis()}.pdf"
            val outputDir = File(context.filesDir, "generated_pdfs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, filename)

            val json = JSONObject().apply {
                put("invoice_no", data.invoiceNo)
                put("date", data.date)
                put("due_date", data.dueDate)
                put("sender_name", data.senderName)
                put("sender_address", data.senderAddress)
                put("sender_email", data.senderEmail)
                put("client_name", data.clientName)
                put("client_address", data.clientAddress)
                put("client_email", data.clientEmail)
                put("tax_rate", data.taxRate)
                put("discount", data.discount)
                put("currency", data.currency)
                put("notes", data.notes)

                val itemsArr = JSONArray()
                for (item in data.items) {
                    val itObj = JSONObject().apply {
                        put("desc", item.desc)
                        put("qty", item.qty)
                        put("rate", item.rate)
                        put("amount", item.amount)
                    }
                    itemsArr.put(itObj)
                }
                put("items", itemsArr)
            }

            val resultStr = module.callAttr("generate_invoice_pdf", json.toString(), outputFile.absolutePath).toString()
            parseGenerationResult(resultStr, outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating invoice", e)
            GenerationResult(
                success = false,
                engineUsed = "Failed",
                outputPath = "",
                fileSizeBytes = 0,
                durationMs = 0,
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun generateReport(
        context: Context,
        data: ReportFormData
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val cleanTitle = data.title.take(20).replace(Regex("[^a-zA-Z0-9_]"), "_")
            val filename = "Report_${cleanTitle}_${System.currentTimeMillis()}.pdf"
            val outputDir = File(context.filesDir, "generated_pdfs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, filename)

            val json = JSONObject().apply {
                put("title", data.title)
                put("subtitle", data.subtitle)
                put("author", data.author)
                put("date", data.date)

                val secArr = JSONArray()
                for (sec in data.sections) {
                    val sObj = JSONObject().apply {
                        put("heading", sec.heading)
                        put("body", sec.body)
                    }
                    secArr.put(sObj)
                }
                put("sections", secArr)
            }

            val resultStr = module.callAttr("generate_report_pdf", json.toString(), outputFile.absolutePath).toString()
            parseGenerationResult(resultStr, outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating report", e)
            GenerationResult(
                success = false,
                engineUsed = "Failed",
                outputPath = "",
                fileSizeBytes = 0,
                durationMs = 0,
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun generateCertificate(
        context: Context,
        data: CertificateFormData
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val cleanName = data.recipientName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val filename = "Certificate_${cleanName}_${System.currentTimeMillis()}.pdf"
            val outputDir = File(context.filesDir, "generated_pdfs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, filename)

            val json = JSONObject().apply {
                put("recipient_name", data.recipientName)
                put("title", data.title)
                put("subtitle", data.subtitle)
                put("course_or_achievement", data.courseOrAchievement)
                put("date", data.date)
                put("issuer", data.issuer)
                put("signatory", data.signatory)
            }

            val resultStr = module.callAttr("generate_certificate_pdf", json.toString(), outputFile.absolutePath).toString()
            parseGenerationResult(resultStr, outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating certificate", e)
            GenerationResult(
                success = false,
                engineUsed = "Failed",
                outputPath = "",
                fileSizeBytes = 0,
                durationMs = 0,
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun generateFromHtml(
        context: Context,
        htmlContent: String,
        cssContent: String,
        title: String = "Custom Document",
        enginePreference: String = "auto"
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val filename = "Doc_${System.currentTimeMillis()}.pdf"
            val outputDir = File(context.filesDir, "generated_pdfs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, filename)

            val resultStr = module.callAttr(
                "generate_pdf_from_html",
                htmlContent,
                cssContent,
                outputFile.absolutePath,
                enginePreference
            ).toString()

            val res = parseGenerationResult(resultStr, outputFile.absolutePath)
            res.copy(
                title = title.ifBlank { "Custom HTML Document" },
                docType = "CUSTOM_HTML",
                summary = "Generated from custom HTML (${htmlContent.length} chars)",
                sourceContent = htmlContent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating from HTML", e)
            GenerationResult(
                success = false,
                engineUsed = "Failed",
                outputPath = "",
                fileSizeBytes = 0,
                durationMs = 0,
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun executeScript(
        context: Context,
        scriptCode: String
    ): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val module = getModule() ?: throw IllegalStateException("Python runtime is not available")
            val filename = "ScriptOutput_${System.currentTimeMillis()}.pdf"
            val outputDir = File(context.filesDir, "generated_pdfs")
            outputDir.mkdirs()
            val outputFile = File(outputDir, filename)

            val resultStr = module.callAttr(
                "execute_python_code",
                scriptCode,
                outputFile.absolutePath
            ).toString()

            val json = JSONObject(resultStr)
            val success = json.optBoolean("success", false)
            val stdout = json.optString("stdout", "")
            val stderr = json.optString("stderr", "")
            val durationMs = json.optLong("duration_ms", 0)
            val pdfCreated = json.optBoolean("pdf_created", false)
            val path = if (pdfCreated) outputFile.absolutePath else null
            val size = if (pdfCreated && outputFile.exists()) outputFile.length() else 0L

            ExecutionResult(
                success = success,
                stdout = stdout,
                stderr = stderr,
                durationMs = durationMs,
                pdfCreated = pdfCreated,
                outputPdfPath = path,
                fileSizeBytes = size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing script", e)
            ExecutionResult(
                success = false,
                stdout = "",
                stderr = e.message ?: "Unknown Python execution error",
                durationMs = 0,
                pdfCreated = false,
                outputPdfPath = null,
                fileSizeBytes = 0
            )
        }
    }

    private fun parseGenerationResult(jsonStr: String, fallbackPath: String): GenerationResult {
        return try {
            val obj = JSONObject(jsonStr)
            val success = obj.optBoolean("success", false)
            val engine = obj.optString("engine_used", "Unknown")
            val path = obj.optString("output_path", fallbackPath)
            val size = obj.optLong("file_size", 0)
            val duration = obj.optLong("duration_ms", 0)
            val weasyAttempted = obj.optBoolean("weasyprint_attempted", false)
            val weasyError = if (obj.has("weasyprint_error") && !obj.isNull("weasyprint_error")) obj.getString("weasyprint_error") else null
            val error = if (obj.has("error") && !obj.isNull("error")) obj.getString("error") else null
            val title = obj.optString("title", "Document")
            val docType = obj.optString("doc_type", "DOCUMENT")
            val summary = obj.optString("summary", "")
            val src = obj.optString("source_content", "")

            GenerationResult(
                success = success,
                engineUsed = engine,
                outputPath = path,
                fileSizeBytes = size,
                durationMs = duration,
                weasyprintAttempted = weasyAttempted,
                weasyprintError = weasyError,
                error = error,
                title = title,
                docType = docType,
                summary = summary,
                sourceContent = src
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing generation result: $jsonStr", e)
            GenerationResult(
                success = false,
                engineUsed = "Error",
                outputPath = fallbackPath,
                fileSizeBytes = 0,
                durationMs = 0,
                error = e.message
            )
        }
    }
}
