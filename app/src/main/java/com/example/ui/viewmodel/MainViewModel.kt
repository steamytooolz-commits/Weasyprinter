package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PyPrintApp
import com.example.data.model.GeneratedDocument
import com.example.python.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PyPrintApp).repository

    // Documents from Room
    val documents: StateFlow<List<GeneratedDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Navigation Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Engine Diagnostics
    private val _diagnostics = MutableStateFlow<EngineDiagnostics?>(null)
    val diagnostics: StateFlow<EngineDiagnostics?> = _diagnostics.asStateFlow()

    // Loading & Generation State
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _lastGenerationResult = MutableStateFlow<GenerationResult?>(null)
    val lastGenerationResult: StateFlow<GenerationResult?> = _lastGenerationResult.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()

    // Active PDF document for the viewer dialog
    private val _viewingDoc = MutableStateFlow<GeneratedDocument?>(null)
    val viewingDoc: StateFlow<GeneratedDocument?> = _viewingDoc.asStateFlow()

    // Form data
    val invoiceForm = MutableStateFlow(InvoiceFormData())
    val reportForm = MutableStateFlow(ReportFormData())
    val certificateForm = MutableStateFlow(CertificateFormData())

    // HTML Studio state
    val htmlCode = MutableStateFlow(DEFAULT_HTML_TEMPLATE)
    val cssCode = MutableStateFlow(DEFAULT_CSS_TEMPLATE)
    val customDocTitle = MutableStateFlow("Modern Python Invoice")
    val selectedEnginePreference = MutableStateFlow("auto")

    // Python REPL state
    val pythonScript: MutableStateFlow<String> = MutableStateFlow(DEFAULT_PYTHON_SCRIPT)

    init {
        loadDiagnostics()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun openViewer(doc: GeneratedDocument) {
        _viewingDoc.value = doc
    }

    fun closeViewer() {
        _viewingDoc.value = null
    }

    fun clearLastResult() {
        _lastGenerationResult.value = null
    }

    fun loadDiagnostics() {
        viewModelScope.launch {
            _diagnostics.value = PythonBridge.getEngineDiagnostics()
        }
    }

    fun generateInvoice() {
        viewModelScope.launch {
            _isGenerating.value = true
            _lastGenerationResult.value = null
            try {
                val result = PythonBridge.generateInvoice(getApplication(), invoiceForm.value)
                _lastGenerationResult.value = result
                if (result.success) {
                    val entity = GeneratedDocument(
                        title = result.title.ifBlank { "Invoice #${invoiceForm.value.invoiceNo}" },
                        docType = "INVOICE",
                        filePath = result.outputPath,
                        fileSizeBytes = result.fileSizeBytes,
                        engineUsed = result.engineUsed,
                        durationMs = result.durationMs,
                        summary = result.summary,
                        sourceContent = result.sourceContent
                    )
                    val id = repository.insertDocument(entity)
                    _viewingDoc.value = entity.copy(id = id)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateReport() {
        viewModelScope.launch {
            _isGenerating.value = true
            _lastGenerationResult.value = null
            try {
                val result = PythonBridge.generateReport(getApplication(), reportForm.value)
                _lastGenerationResult.value = result
                if (result.success) {
                    val entity = GeneratedDocument(
                        title = result.title.ifBlank { reportForm.value.title },
                        docType = "REPORT",
                        filePath = result.outputPath,
                        fileSizeBytes = result.fileSizeBytes,
                        engineUsed = result.engineUsed,
                        durationMs = result.durationMs,
                        summary = result.summary,
                        sourceContent = result.sourceContent
                    )
                    val id = repository.insertDocument(entity)
                    _viewingDoc.value = entity.copy(id = id)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateCertificate() {
        viewModelScope.launch {
            _isGenerating.value = true
            _lastGenerationResult.value = null
            try {
                val result = PythonBridge.generateCertificate(getApplication(), certificateForm.value)
                _lastGenerationResult.value = result
                if (result.success) {
                    val entity = GeneratedDocument(
                        title = result.title.ifBlank { "Certificate - ${certificateForm.value.recipientName}" },
                        docType = "CERTIFICATE",
                        filePath = result.outputPath,
                        fileSizeBytes = result.fileSizeBytes,
                        engineUsed = result.engineUsed,
                        durationMs = result.durationMs,
                        summary = result.summary,
                        sourceContent = result.sourceContent
                    )
                    val id = repository.insertDocument(entity)
                    _viewingDoc.value = entity.copy(id = id)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateCustomHtml() {
        viewModelScope.launch {
            _isGenerating.value = true
            _lastGenerationResult.value = null
            try {
                val result = PythonBridge.generateFromHtml(
                    context = getApplication(),
                    htmlContent = htmlCode.value,
                    cssContent = cssCode.value,
                    title = customDocTitle.value,
                    enginePreference = selectedEnginePreference.value
                )
                _lastGenerationResult.value = result
                if (result.success) {
                    val entity = GeneratedDocument(
                        title = customDocTitle.value.ifBlank { "Custom Document" },
                        docType = "CUSTOM_HTML",
                        filePath = result.outputPath,
                        fileSizeBytes = result.fileSizeBytes,
                        engineUsed = result.engineUsed,
                        durationMs = result.durationMs,
                        summary = result.summary,
                        sourceContent = htmlCode.value
                    )
                    val id = repository.insertDocument(entity)
                    _viewingDoc.value = entity.copy(id = id)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun runPythonScript() {
        viewModelScope.launch {
            _isGenerating.value = true
            _lastExecutionResult.value = null
            try {
                val execResult = PythonBridge.executeScript(getApplication(), pythonScript.value)
                _lastExecutionResult.value = execResult
                if (execResult.pdfCreated && execResult.outputPdfPath != null) {
                    val entity = GeneratedDocument(
                        title = "Python Script Output",
                        docType = "PYTHON_SCRIPT",
                        filePath = execResult.outputPdfPath,
                        fileSizeBytes = execResult.fileSizeBytes,
                        engineUsed = "Python Custom Script",
                        durationMs = execResult.durationMs,
                        summary = "Generated dynamically via Chaquopy Python REPL",
                        sourceContent = pythonScript.value
                    )
                    val id = repository.insertDocument(entity)
                    _viewingDoc.value = entity.copy(id = id)
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteDocument(doc: GeneratedDocument) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (_viewingDoc.value?.id == doc.id) {
                _viewingDoc.value = null
            }
        }
    }

    fun loadSampleTemplate(type: String) {
        when (type) {
            "invoice" -> {
                htmlCode.value = DEFAULT_HTML_TEMPLATE
                cssCode.value = DEFAULT_CSS_TEMPLATE
                customDocTitle.value = "Modern Python Invoice"
            }
            "badge" -> {
                htmlCode.value = BADGE_HTML_TEMPLATE
                cssCode.value = BADGE_CSS_TEMPLATE
                customDocTitle.value = "Developer Conference Pass"
            }
            "receipt" -> {
                htmlCode.value = RECEIPT_HTML_TEMPLATE
                cssCode.value = RECEIPT_CSS_TEMPLATE
                customDocTitle.value = "POS Terminal Receipt"
            }
            "letter" -> {
                htmlCode.value = LETTER_HTML_TEMPLATE
                cssCode.value = LETTER_CSS_TEMPLATE
                customDocTitle.value = "Formal Recommendation Letter"
            }
        }
    }

    fun loadReplExample(type: String) {
        when (type) {
            "fpdf2" -> pythonScript.value = FPDF2_EXAMPLE_SCRIPT
            "reportlab" -> pythonScript.value = REPORTLAB_EXAMPLE_SCRIPT
            "weasyprint" -> pythonScript.value = WEASYPRINT_EXAMPLE_SCRIPT
            "jinja" -> pythonScript.value = JINJA_EXAMPLE_SCRIPT
        }
    }

    companion object {
        const val DEFAULT_HTML_TEMPLATE = """<div class="invoice-box">
  <div class="header">
    <div class="logo">PyPrint</div>
    <div class="status">PAID</div>
  </div>
  <div class="title">INVOICE #INV-8829</div>
  <p class="subtitle">Billed to: Apex Digital Labs</p>

  <table class="data-table">
    <tr>
      <th>Description</th>
      <th style="text-align:right;">Amount</th>
    </tr>
    <tr>
      <td>Python Architecture & Engine Optimization</td>
      <td style="text-align:right;">$3,200.00</td>
    </tr>
    <tr>
      <td>WeasyPrint Mobile Layout Template</td>
      <td style="text-align:right;">$1,450.00</td>
    </tr>
    <tr>
      <td>Chaquopy JNI Bridge Implementation</td>
      <td style="text-align:right;">$2,100.00</td>
    </tr>
    <tr class="total-row">
      <td>Total Amount Due</td>
      <td style="text-align:right;">$6,750.00</td>
    </tr>
  </table>

  <div class="footer-note">
    Generated with Python WeasyPrint on Android via Chaquopy.
  </div>
</div>"""

        const val DEFAULT_CSS_TEMPLATE = """@page {
  size: A4;
  margin: 15mm;
}
body {
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #1e293b;
  margin: 0;
  padding: 10px;
}
.invoice-box {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  background-color: #ffffff;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #0284c7;
  padding-bottom: 12px;
  margin-bottom: 20px;
}
.logo {
  font-size: 24px;
  font-weight: bold;
  color: #0369a1;
}
.status {
  background-color: #dcfce7;
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 20px;
}
.title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}
.subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 24px;
}
.data-table th {
  background-color: #f1f5f9;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
  padding: 10px;
  text-align: left;
}
.data-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}
.total-row td {
  font-size: 16px;
  font-weight: bold;
  color: #0369a1;
  border-top: 2px solid #cbd5e1;
  border-bottom: none;
  padding-top: 16px;
}
.footer-note {
  margin-top: 36px;
  font-size: 11px;
  color: #94a3b8;
  text-align: center;
  border-top: 1px dashed #cbd5e1;
  padding-top: 12px;
}"""

        const val BADGE_HTML_TEMPLATE = """<div class="pass-card">
  <div class="badge-head">PYTHON DEVCON 2026</div>
  <div class="badge-type">ALL-ACCESS VIP PASS</div>
  <h1 class="attendee-name">SARAH CHEN</h1>
  <p class="role">Principal Systems Engineer • PyFoundation</p>
  <div class="qr-placeholder">[ OFFICIAL DIGITAL BADGE: #PY-9042 ]</div>
  <div class="track">TRACK: EMBEDDED PYTHON &amp; MOBILE RUNTIMES</div>
</div>"""

        const val BADGE_CSS_TEMPLATE = """@page {
  size: 105mm 148mm;
  margin: 10mm;
}
body {
  font-family: 'Segoe UI', Arial, sans-serif;
  text-align: center;
  background: #0f172a;
  color: #ffffff;
  padding: 10px;
}
.pass-card {
  border: 3px solid #38bdf8;
  border-radius: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #1e293b, #0f172a);
}
.badge-head {
  color: #f59e0b;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 2px;
}
.badge-type {
  display: inline-block;
  background: #0284c7;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 20px;
  margin: 12px 0;
}
.attendee-name {
  font-size: 24px;
  margin: 12px 0 4px 0;
  letter-spacing: 1px;
}
.role {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 24px;
}
.qr-placeholder {
  border: 2px dashed #475569;
  border-radius: 8px;
  padding: 20px 10px;
  font-size: 12px;
  font-family: monospace;
  color: #38bdf8;
  background: #020617;
}
.track {
  font-size: 10px;
  color: #64748b;
  margin-top: 20px;
}"""

        const val RECEIPT_HTML_TEMPLATE = """<div class="receipt">
  <div class="store">PY-COFFEE ROASTERS</div>
  <div class="sub">124 Main Street, Austin TX</div>
  <div class="line">---------------------------------</div>
  <div class="item-row"><span>1x Pour Over (Ethiopia)</span><span>$5.50</span></div>
  <div class="item-row"><span>1x Cold Brew Nitro</span><span>$6.00</span></div>
  <div class="item-row"><span>1x Almond Croissant</span><span>$4.75</span></div>
  <div class="line">---------------------------------</div>
  <div class="item-row total"><span>TOTAL</span><span>$16.25</span></div>
  <div class="line">---------------------------------</div>
  <div class="footer">Thank you for visiting!</div>
</div>"""

        const val RECEIPT_CSS_TEMPLATE = """@page {
  size: 80mm 150mm;
  margin: 5mm;
}
body {
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  color: #000;
  line-height: 1.4;
}
.receipt {
  padding: 8px;
}
.store {
  font-weight: bold;
  font-size: 14px;
  text-align: center;
}
.sub {
  font-size: 10px;
  text-align: center;
  color: #444;
}
.line {
  text-align: center;
  margin: 6px 0;
}
.item-row {
  display: flex;
  justify-content: space-between;
  margin: 4px 0;
}
.total {
  font-weight: bold;
  font-size: 14px;
}
.footer {
  text-align: center;
  margin-top: 14px;
  font-size: 10px;
}"""

        const val LETTER_HTML_TEMPLATE = """<div class="letterhead">
  <h1>GLOBAL INNOVATIONS CORP</h1>
  <p>750 Technology Parkway • New York, NY</p>
</div>
<div class="date">September 23, 2026</div>
<p>To Whom It May Concern,</p>
<p>I am pleased to provide this formal recommendation for the PyPrint mobile engineering initiative. Over the past development cycle, the team demonstrated outstanding mastery in bridging native CPython runtimes with Android modern UI frameworks.</p>
<p>Their architecture enables offline document compilation, sub-second PDF generation, and strict data confidentiality without relying on cloud computation.</p>
<div class="closing">
  <p>Sincerely,</p>
  <b>David Vance, VP of Engineering</b>
</div>"""

        const val LETTER_CSS_TEMPLATE = """@page {
  size: letter;
  margin: 25mm;
}
body {
  font-family: Georgia, serif;
  font-size: 12pt;
  line-height: 1.6;
  color: #111;
}
.letterhead {
  border-bottom: 2px solid #333;
  padding-bottom: 12px;
  margin-bottom: 30px;
}
.letterhead h1 {
  font-size: 18pt;
  margin: 0;
  color: #0369a1;
}
.letterhead p {
  font-size: 10pt;
  color: #666;
  margin: 4px 0 0 0;
}
.date {
  margin-bottom: 20px;
  font-style: italic;
}
.closing {
  margin-top: 40px;
}"""

        const val FPDF2_EXAMPLE_SCRIPT = """# Generate a colorful PDF using FPDF2 in Python
from fpdf import FPDF
import time

print("Starting FPDF2 document compilation...")

pdf = FPDF()
pdf.add_page()
pdf.set_font('Helvetica', 'B', 18)
pdf.set_text_color(3, 105, 161) # Python Royal Blue

# Title
pdf.cell(0, 14, 'PyPrint Interactive Python Canvas', ln=True, align='C')
pdf.ln(4)

# Subtitle
pdf.set_font('Helvetica', 'I', 11)
pdf.set_text_color(100, 116, 139)
pdf.cell(0, 8, 'Generated directly with FPDF2 inside Chaquopy on Android', ln=True, align='C')
pdf.ln(10)

# Colored Metrics Box
pdf.set_fill_color(240, 249, 255)
pdf.set_draw_color(186, 230, 253)
pdf.rect(x=15, y=pdf.get_y(), w=180, h=40, style='FD')

pdf.set_xy(20, pdf.get_y() + 8)
pdf.set_font('Helvetica', 'B', 12)
pdf.set_text_color(15, 23, 42)
pdf.cell(85, 8, 'Runtime: CPython 3.11 Embedded')
pdf.cell(85, 8, 'Bridge: Chaquopy JNI Native', ln=True)

pdf.set_xy(20, pdf.get_y() + 6)
pdf.cell(85, 8, 'Security: 100% Offline Generation')
pdf.cell(85, 8, 'Target: Direct-to-Device Storage', ln=True)

pdf.set_y(pdf.get_y() + 18)
pdf.set_font('Helvetica', '', 10)
pdf.set_text_color(51, 65, 85)

for i in range(1, 6):
    pdf.cell(0, 7, f"- Item {i}: Processed benchmark step with timestamp {time.strftime('%H:%M:%S')}", ln=True)

if output_pdf_path:
    pdf.output(output_pdf_path)
    print(f"Success: PDF generated at {output_pdf_path}")
"""

        const val REPORTLAB_EXAMPLE_SCRIPT = """# Generate a structured document with ReportLab Flowables
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors

print("Initializing ReportLab document generator...")

if output_pdf_path:
    doc = SimpleDocTemplate(output_pdf_path, pagesize=letter, rightMargin=36, leftMargin=36, topMargin=36, bottomMargin=36)
    styles = getSampleStyleSheet()
    story = []

    title_style = ParagraphStyle(
        'MainTitle',
        parent=styles['Title'],
        fontSize=22,
        textColor=colors.HexColor('#0369A1'),
        spaceAfter=12
    )
    story.append(Paragraph("ReportLab Document Architecture", title_style))
    story.append(Paragraph("Compiled natively on Android with Chaquopy", styles['Italic']))
    story.append(Spacer(1, 14))

    data = [
        ["Module", "Type", "Status", "Execution"],
        ["WeasyPrint", "HTML to PDF", "Installed", "Fast"],
        ["ReportLab", "Flowable Engine", "Active", "Sub-second"],
        ["FPDF2", "Pure Python", "Active", "Lightweight"],
        ["Jinja2", "Template Engine", "Active", "Dynamic"]
    ]

    t = Table(data, colWidths=[130, 110, 110, 110])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0284C7')),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('FONTNAME', (0,0), (-1,0), 'Helvetica-Bold'),
        ('BOTTOMPADDING', (0,0), (-1,0), 8),
        ('GRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.HexColor('#F8FAFC'), colors.white])
    ]))

    story.append(t)
    doc.build(story)
    print(f"ReportLab successfully built PDF: {output_pdf_path}")
"""

        const val WEASYPRINT_EXAMPLE_SCRIPT = """# WeasyPrint HTML & CSS to PDF Engine
import weasyprint

print("WeasyPrint engine:", weasyprint.VERSION_STRING)

html = '''<!DOCTYPE html>
<html>
<head>
<style>
  @page { size: A4; margin: 18mm; }
  h1 { color: #0284C7; font-family: sans-serif; }
  p { color: #334155; font-size: 14px; font-family: sans-serif; line-height: 1.5; }
  .badge { background: #E0F2FE; color: #0369A1; padding: 4px 8px; border-radius: 4px; font-weight: bold; }
</style>
</head>
<body>
  <h1>WeasyPrint Android Engine</h1>
  <p><span class="badge">Active</span> HTML and CSS compiled into PDF.</p>
  <p>Rendered natively with pure Python on Android via Chaquopy.</p>
</body>
</html>'''

if output_pdf_path:
    doc = weasyprint.HTML(string=html)
    doc.write_pdf(target=output_pdf_path)
    print("Success! WeasyPrint PDF created at:", output_pdf_path)
"""

        const val JINJA_EXAMPLE_SCRIPT = """# Jinja2 Template Rendering Test
import jinja2
import json

template_str = '''
<h1>Welcome, {{ user.name }}!</h1>
<p>Your subscription is {{ user.plan }} (Active until {{ user.renewal }}).</p>
<ul>
{% for perk in perks %}
  <li>{{ perk }}</li>
{% endfor %}
</ul>
'''

context = {
    "user": {"name": "Jordan Lee", "plan": "Enterprise Pro", "renewal": "2027-01-01"},
    "perks": ["Unlimited Python PDF Exports", "Custom WeasyPrint Stylesheets", "Zero Cloud Latency"]
}

t = jinja2.Template(template_str)
rendered = t.render(**context)
print("Rendered HTML via Jinja2:\n", rendered)

if output_pdf_path:
    generate_pdf_from_html(rendered, "", output_pdf_path)
    print("PDF generated successfully from Jinja2 output!")
"""

        const val DEFAULT_PYTHON_SCRIPT = FPDF2_EXAMPLE_SCRIPT
    }
}
