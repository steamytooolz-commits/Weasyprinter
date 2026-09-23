# -*- coding: utf-8 -*-
"""
PyPrint PDF Engine
Robust Python PDF Generation & Document Studio for Android via Chaquopy.
Integrates WeasyPrint, ReportLab, FPDF2, Jinja2, and Markdown.
"""

import sys
import os
import json
import traceback
import io
import time

# Android SELinux Hardening & Subprocess Safety
# Standard POSIX ctypes.util.find_library invokes /sbin/ldconfig, gcc, or objdump via subprocess,
# or scans restricted system/vendor directories.
# In Android's untrusted_app SELinux domain, searching /vendor or spawning these utilities
# violates SELinux policy, flooding the kernel audit buffer and triggering "audit: rate limit exceeded".
# We immediately neutralize external subprocess search and return None safely in-process.
try:
    from weasyprint import native_resolver
    native_resolver.resolve_native_libraries()
except Exception:
    pass

try:
    import subprocess
    if not getattr(subprocess, "_is_safe_popen", False):
        _orig_popen = subprocess.Popen

        def _safe_popen(*args, **kwargs):
            cmd = args[0] if args else kwargs.get("args", [])
            if isinstance(cmd, (list, tuple)) and cmd:
                first = str(cmd[0])
            else:
                first = str(cmd).split()[0] if str(cmd).strip() else ""

            forbidden = ("ldconfig", "gcc", "ld", "objdump")
            if any(fb in first for fb in forbidden):
                raise FileNotFoundError(f"Utility not available on Android: {first}")

            return _orig_popen(*args, **kwargs)

        subprocess.Popen = _safe_popen
        subprocess._is_safe_popen = True
except Exception:
    pass

# Diagnostic detection of available packages
def get_engine_diagnostics():
    diagnostics = {
        "python_version": sys.version,
        "platform": sys.platform,
        "executable": sys.executable,
        "packages": {}
    }

    # Test WeasyPrint
    try:
        import weasyprint
        diagnostics["packages"]["weasyprint"] = {
            "available": True,
            "version": getattr(weasyprint, "__version__", "unknown"),
            "error": None
        }
    except Exception as e:
        diagnostics["packages"]["weasyprint"] = {
            "available": False,
            "version": None,
            "error": f"{type(e).__name__}: {str(e)}"
        }

    # Test ReportLab
    try:
        import reportlab
        diagnostics["packages"]["reportlab"] = {
            "available": True,
            "version": getattr(reportlab, "__version__", "unknown"),
            "error": None
        }
    except Exception as e:
        diagnostics["packages"]["reportlab"] = {
            "available": False,
            "version": None,
            "error": f"{type(e).__name__}: {str(e)}"
        }

    # Test FPDF2
    try:
        import fpdf
        diagnostics["packages"]["fpdf2"] = {
            "available": True,
            "version": getattr(fpdf, "__version__", "unknown"),
            "error": None
        }
    except Exception as e:
        diagnostics["packages"]["fpdf2"] = {
            "available": False,
            "version": None,
            "error": f"{type(e).__name__}: {str(e)}"
        }

    # Test Jinja2
    try:
        import jinja2
        diagnostics["packages"]["jinja2"] = {
            "available": True,
            "version": getattr(jinja2, "__version__", "unknown"),
            "error": None
        }
    except Exception as e:
        diagnostics["packages"]["jinja2"] = {
            "available": False,
            "version": None,
            "error": f"{type(e).__name__}: {str(e)}"
        }

    # Test Markdown
    try:
        import markdown
        diagnostics["packages"]["markdown"] = {
            "available": True,
            "version": getattr(markdown, "__version__", "unknown"),
            "error": None
        }
    except Exception as e:
        diagnostics["packages"]["markdown"] = {
            "available": False,
            "version": None,
            "error": f"{type(e).__name__}: {str(e)}"
        }

    return json.dumps(diagnostics)


def render_html_with_jinja(template_str, context_json_str):
    """Renders an HTML template with JSON context using Jinja2."""
    try:
        import jinja2
        context = json.loads(context_json_str) if isinstance(context_json_str, str) else context_json_str
        template = jinja2.Template(template_str)
        return template.render(**context)
    except Exception as e:
        return f"<!-- Jinja2 Rendering Error: {str(e)} -->\n{template_str}"


def generate_pdf_fpdf(html_content, output_path, title="Document"):
    """Generates a PDF using FPDF2 HTML2PDF parser as a dependable engine."""
    from fpdf import FPDF, HTMLMixin

    class CustomPDF(FPDF, HTMLMixin):
        def header(self):
            self.set_font('Helvetica', 'B', 8)
            self.set_text_color(130, 140, 160)
            self.cell(0, 8, f"PyPrint Studio  |  {title}", border=0, align='L')
            self.ln(6)
            self.set_draw_color(225, 230, 240)
            self.line(self.l_margin, self.get_y(), self.w - self.r_margin, self.get_y())
            self.ln(4)

        def footer(self):
            self.set_y(-12)
            self.set_font('Helvetica', 'I', 8)
            self.set_text_color(150, 150, 160)
            self.cell(0, 8, f"Page {self.page_no()} / {{nb}}  |  Generated with Python on Android", align='C')

    pdf = CustomPDF()
    pdf.alias_nb_pages()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()
    pdf.set_font("Helvetica", size=10)

    import re
    clean_html = re.sub(r'<!DOCTYPE[^>]*>', '', html_content, flags=re.IGNORECASE)
    clean_html = re.sub(r'<!--.*?-->', '', clean_html, flags=re.DOTALL)
    clean_html = re.sub(r'<style[^>]*>.*?</style>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
    clean_html = re.sub(r'<script[^>]*>.*?</script>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
    clean_html = re.sub(r'<head[^>]*>.*?</head>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
    clean_html = re.sub(r'<meta[^>]*>', '', clean_html, flags=re.IGNORECASE)
    clean_html = re.sub(r'<link[^>]*>', '', clean_html, flags=re.IGNORECASE)

    try:
        pdf.write_html(clean_html)
    except Exception:
        # Fallback to plain text if HTML contains unsupported tags
        clean_text = re.sub(r'<[^>]+>', ' ', clean_html)
        clean_text = '\n'.join([line.strip() for line in clean_text.splitlines() if line.strip()])
        pdf.multi_cell(0, 6, clean_text)

    pdf.output(output_path)
    return True


def generate_pdf_reportlab(html_content, output_path, title="Document"):
    """Generates a PDF using ReportLab Flowables and Styles."""
    from reportlab.lib.pagesizes import letter
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib import colors
    import re

    doc = SimpleDocTemplate(
        output_path,
        pagesize=letter,
        rightMargin=36,
        leftMargin=36,
        topMargin=40,
        bottomMargin=40
    )
    story = []
    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        'PyPrintTitle',
        parent=styles['Heading1'],
        fontSize=20,
        leading=24,
        textColor=colors.HexColor('#0F172A'),
        spaceAfter=12
    )

    body_style = ParagraphStyle(
        'PyPrintBody',
        parent=styles['Normal'],
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#334155')
    )

    story.append(Paragraph(title, title_style))
    story.append(Spacer(1, 10))

    # Parse simple paragraphs from HTML
    paragraphs = re.findall(r'<p[^>]*>(.*?)</p>', html_content, re.DOTALL | re.IGNORECASE)
    if not paragraphs:
        paragraphs = [re.sub(r'<[^>]+>', ' ', html_content)]

    for p in paragraphs:
        clean = re.sub(r'<[^>]+>', '', p).strip()
        if clean:
            story.append(Paragraph(clean, body_style))
            story.append(Spacer(1, 8))

    doc.build(story)
    return True


def generate_pdf_from_html(html_content, css_content, output_path, engine_preference="auto"):
    """
    Primary PDF generation function.
    Tries WeasyPrint first if requested or in auto mode.
    If WeasyPrint has missing C-level font/shaping libraries on Android,
    gracefully cascades to ReportLab / FPDF2 without failing the user.
    """
    result = {
        "success": False,
        "engine_used": "",
        "output_path": output_path,
        "file_size": 0,
        "duration_ms": 0,
        "weasyprint_attempted": False,
        "weasyprint_error": None,
        "error": None
    }

    start_time = time.time()
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)

    full_html = html_content
    if css_content and "<style>" not in html_content:
        full_html = f"<!DOCTYPE html><html><head><meta charset='utf-8'><style>{css_content}</style></head><body>{html_content}</body></html>"

    # Attempt WeasyPrint
    if engine_preference in ("auto", "weasyprint"):
        result["weasyprint_attempted"] = True
        try:
            import weasyprint
            # Weasyprint HTML rendering
            wp_html = weasyprint.HTML(string=full_html)
            wp_html.write_pdf(target=output_path)
            result["success"] = True
            result["engine_used"] = "WeasyPrint"
        except Exception as e:
            err_msg = f"{type(e).__name__}: {str(e)}"
            result["weasyprint_error"] = err_msg

    # Fallback to FPDF2 or ReportLab if WeasyPrint wasn't successful
    if not result["success"]:
        try:
            generate_pdf_fpdf(full_html, output_path, title="PyPrint Document")
            result["success"] = True
            result["engine_used"] = "FPDF2 (Python Engine)"
        except Exception as fpdf_err:
            try:
                generate_pdf_reportlab(full_html, output_path, title="PyPrint Document")
                result["success"] = True
                result["engine_used"] = "ReportLab (Python Engine)"
            except Exception as rl_err:
                result["error"] = f"Generation failed on all engines: FPDF2: {str(fpdf_err)} | ReportLab: {str(rl_err)}"
                result["success"] = False

    result["duration_ms"] = int((time.time() - start_time) * 1000)
    if result["success"] and os.path.exists(output_path):
        result["file_size"] = os.path.getsize(output_path)

    return json.dumps(result)


def generate_invoice_pdf(invoice_data_json_str, output_path):
    """
    Generates a high-fidelity business invoice PDF.
    invoice_data_json_str: JSON with invoice_number, date, client_name, client_email,
                           items (list of {desc, qty, rate, total}), tax, discount, notes.
    """
    start_time = time.time()
    data = json.loads(invoice_data_json_str) if isinstance(invoice_data_json_str, str) else invoice_data_json_str

    invoice_no = data.get("invoice_no", "INV-2026-001")
    invoice_date = data.get("date", "2026-09-23")
    due_date = data.get("due_date", "2026-10-23")
    sender_name = data.get("sender_name", "TechPulse Innovations LLC")
    sender_address = data.get("sender_address", "100 Innovation Way, Suite 400\nSan Francisco, CA 94105")
    sender_email = data.get("sender_email", "billing@techpulse.io")

    client_name = data.get("client_name", "Acme Corporation")
    client_address = data.get("client_address", "456 Enterprise Blvd\nAustin, TX 78701")
    client_email = data.get("client_email", "accounts@acme.corp")

    items = data.get("items", [
        {"desc": "Full-Stack Android Python Architecture", "qty": 1, "rate": 2500.0, "amount": 2500.0},
        {"desc": "WeasyPrint & Chaquopy Engine Pipeline", "qty": 1, "rate": 1800.0, "amount": 1800.0},
        {"desc": "Automated PDF Reporting Module", "qty": 2, "rate": 650.0, "amount": 1300.0}
    ])

    subtotal = sum(float(it.get("amount", float(it.get("qty", 1)) * float(it.get("rate", 0)))) for it in items)
    tax_rate = float(data.get("tax_rate", 8.25))
    tax_amount = round(subtotal * (tax_rate / 100.0), 2)
    discount = float(data.get("discount", 0.0))
    total_amount = round(subtotal + tax_amount - discount, 2)
    currency = data.get("currency", "$")
    notes = data.get("notes", "Thank you for your business! Payment is due within 30 days via ACH or Wire Transfer.")

    # HTML template with modern CSS paged media
    items_html = ""
    for idx, item in enumerate(items, 1):
        bg = "#F8FAFC" if idx % 2 == 0 else "#FFFFFF"
        qty = item.get("qty", 1)
        rate = float(item.get("rate", 0))
        amt = float(item.get("amount", qty * rate))
        items_html += f"""
        <tr style="background-color: {bg};">
            <td style="padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13px; color: #1E293B;">{item.get('desc', '')}</td>
            <td style="padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13px; text-align: center; color: #475569;">{qty}</td>
            <td style="padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13px; text-align: right; color: #475569;">{currency}{rate:,.2f}</td>
            <td style="padding: 10px 12px; border-bottom: 1px solid #E2E8F0; font-size: 13px; text-align: right; font-weight: 600; color: #0F172A;">{currency}{amt:,.2f}</td>
        </tr>
        """

    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            @page {{
                size: A4;
                margin: 20mm;
            }}
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                color: #0F172A;
                line-height: 1.5;
                margin: 0;
                padding: 0;
            }}
            .header {{
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                padding-bottom: 24px;
                border-bottom: 2px solid #0284C7;
            }}
            .brand {{
                font-size: 24px;
                font-weight: 800;
                color: #0369A1;
                letter-spacing: -0.5px;
            }}
            .invoice-title {{
                text-align: right;
            }}
            .invoice-title h1 {{
                margin: 0;
                font-size: 28px;
                font-weight: 900;
                color: #0F172A;
                letter-spacing: -0.5px;
            }}
            .invoice-tag {{
                display: inline-block;
                background: #E0F2FE;
                color: #0284C7;
                font-weight: 700;
                font-size: 12px;
                padding: 3px 10px;
                border-radius: 4px;
                margin-top: 4px;
            }}
            .parties {{
                display: flex;
                justify-content: space-between;
                margin-top: 28px;
                margin-bottom: 28px;
            }}
            .party-box {{
                width: 48%;
            }}
            .party-label {{
                font-size: 11px;
                font-weight: 700;
                color: #64748B;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                margin-bottom: 6px;
            }}
            .party-name {{
                font-size: 15px;
                font-weight: 700;
                color: #0F172A;
            }}
            .party-detail {{
                font-size: 13px;
                color: #475569;
                white-space: pre-line;
            }}
            .meta-table {{
                width: 100%;
                margin-bottom: 24px;
                background: #F1F5F9;
                border-radius: 6px;
                padding: 12px;
            }}
            table.items-table {{
                width: 100%;
                border-collapse: collapse;
                margin-top: 10px;
            }}
            th {{
                background-color: #0369A1;
                color: #FFFFFF;
                font-size: 12px;
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                padding: 10px 12px;
                text-align: left;
            }}
            .totals-container {{
                display: flex;
                justify-content: flex-end;
                margin-top: 20px;
            }}
            .totals-table {{
                width: 45%;
                border-collapse: collapse;
            }}
            .totals-table td {{
                padding: 6px 12px;
                font-size: 13px;
            }}
            .total-due {{
                background-color: #0369A1;
                color: #FFFFFF;
                font-size: 16px !important;
                font-weight: 800;
                border-radius: 4px;
            }}
            .notes {{
                margin-top: 36px;
                padding: 14px 18px;
                background-color: #F8FAFC;
                border-left: 4px solid #0284C7;
                border-radius: 0 6px 6px 0;
            }}
            .notes-title {{
                font-size: 12px;
                font-weight: 700;
                color: #0369A1;
                text-transform: uppercase;
                margin-bottom: 4px;
            }}
            .notes-text {{
                font-size: 12px;
                color: #475569;
            }}
            .footer {{
                margin-top: 40px;
                text-align: center;
                font-size: 11px;
                color: #94A3B8;
                border-top: 1px solid #E2E8F0;
                padding-top: 12px;
            }}
        </style>
    </head>
    <body>
        <div class="header">
            <div>
                <div class="brand">{sender_name}</div>
                <div class="party-detail">{sender_address}</div>
                <div class="party-detail">{sender_email}</div>
            </div>
            <div class="invoice-title">
                <h1>INVOICE</h1>
                <div class="invoice-tag">#{invoice_no}</div>
                <div style="margin-top: 6px; font-size: 12px; color: #64748B;">Date: <b>{invoice_date}</b></div>
                <div style="font-size: 12px; color: #64748B;">Due: <b>{due_date}</b></div>
            </div>
        </div>

        <div class="parties">
            <div class="party-box">
                <div class="party-label">Billed To</div>
                <div class="party-name">{client_name}</div>
                <div class="party-detail">{client_address}</div>
                <div class="party-detail">{client_email}</div>
            </div>
            <div class="party-box" style="text-align: right;">
                <div class="party-label">Payment Summary</div>
                <div style="font-size: 13px; color: #64748B;">Status: <span style="color: #0284C7; font-weight: 700;">PAYMENT PENDING</span></div>
                <div style="font-size: 13px; color: #64748B;">Currency: <b>{currency} USD</b></div>
            </div>
        </div>

        <table class="items-table">
            <thead>
                <tr>
                    <th style="width: 55%; border-radius: 4px 0 0 0;">Description</th>
                    <th style="width: 15%; text-align: center;">Qty</th>
                    <th style="width: 15%; text-align: right;">Rate</th>
                    <th style="width: 15%; text-align: right; border-radius: 0 4px 0 0;">Amount</th>
                </tr>
            </thead>
            <tbody>
                {items_html}
            </tbody>
        </table>

        <div class="totals-container">
            <table class="totals-table">
                <tr>
                    <td style="color: #64748B;">Subtotal:</td>
                    <td style="text-align: right; font-weight: 600; color: #1E293B;">{currency}{subtotal:,.2f}</td>
                </tr>
                <tr>
                    <td style="color: #64748B;">Tax ({tax_rate}%):</td>
                    <td style="text-align: right; color: #1E293B;">{currency}{tax_amount:,.2f}</td>
                </tr>
                {f'<tr><td style="color: #64748B;">Discount:</td><td style="text-align: right; color: #DC2626;">-{currency}{discount:,.2f}</td></tr>' if discount > 0 else ''}
                <tr class="total-due">
                    <td style="padding: 10px 12px;">Total Due:</td>
                    <td style="text-align: right; padding: 10px 12px;">{currency}{total_amount:,.2f}</td>
                </tr>
            </table>
        </div>

        <div class="notes">
            <div class="notes-title">Payment Terms &amp; Notes</div>
            <div class="notes-text">{notes}</div>
        </div>

        <div class="footer">
            PyPrint Studio  |  Generated natively with Python via Chaquopy on Android
        </div>
    </body>
    </html>
    """

    # Generate using our resilient pipeline
    result_json_str = generate_pdf_from_html(html_content, "", output_path, engine_preference="auto")
    result = json.loads(result_json_str)
    result["title"] = f"Invoice #{invoice_no}"
    result["doc_type"] = "INVOICE"
    result["source_content"] = html_content
    result["summary"] = f"Invoice for {client_name} - Total: {currency}{total_amount:,.2f}"
    return json.dumps(result)


def generate_report_pdf(report_data_json_str, output_path):
    """
    Generates a multi-section executive analytics report PDF.
    """
    data = json.loads(report_data_json_str) if isinstance(report_data_json_str, str) else report_data_json_str

    title = data.get("title", "Quarterly Operations & Performance Report")
    subtitle = data.get("subtitle", "Q3 2026 Executive Summary & Strategic Insights")
    author = data.get("author", "Data Analytics & Engineering Group")
    date_str = data.get("date", "September 23, 2026")
    sections = data.get("sections", [
        {
            "heading": "1. Executive Summary",
            "body": "This report outlines operational benchmarks, Python runtime throughput, and automated document generation efficiency on mobile devices. Through Chaquopy's embedded CPython runtime, on-device generation latency has decreased by 42%."
        },
        {
            "heading": "2. Performance Milestones",
            "body": "System stability achieved 99.98% uptime across all compilation targets. Native ABI bindings for arm64-v8a and x86_64 allow direct binary execution without network roundtrips."
        },
        {
            "heading": "3. Future Architecture Roadmap",
            "body": "Planned enhancements include local font caching, multi-page asynchronous streaming, and accelerated layout computation with pre-compiled CSS stylesheets."
        }
    ])

    sections_html = ""
    for sec in sections:
        sections_html += f"""
        <div style="margin-bottom: 24px;">
            <h2 style="font-size: 16px; font-weight: 700; color: #0369A1; border-bottom: 1px solid #E2E8F0; padding-bottom: 6px; margin-bottom: 10px;">{sec.get('heading', '')}</h2>
            <p style="font-size: 13px; color: #334155; line-height: 1.6; margin: 0;">{sec.get('body', '')}</p>
        </div>
        """

    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            @page {{
                size: letter;
                margin: 24mm 20mm;
            }}
            body {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                color: #0F172A;
                line-height: 1.5;
                margin: 0;
            }}
            .cover-bar {{
                height: 8px;
                background: linear-gradient(90deg, #0284C7, #38BDF8, #F59E0B);
                border-radius: 4px;
                margin-bottom: 24px;
            }}
            .title-area {{
                margin-bottom: 30px;
            }}
            .main-title {{
                font-size: 26px;
                font-weight: 900;
                color: #0F172A;
                margin: 0 0 6px 0;
                letter-spacing: -0.5px;
            }}
            .subtitle {{
                font-size: 15px;
                color: #64748B;
                margin: 0 0 16px 0;
            }}
            .meta-box {{
                display: flex;
                background: #F8FAFC;
                border: 1px solid #E2E8F0;
                border-radius: 8px;
                padding: 12px 18px;
                font-size: 12px;
                color: #475569;
                margin-bottom: 28px;
            }}
            .kpi-row {{
                display: flex;
                gap: 16px;
                margin-bottom: 28px;
            }}
            .kpi-card {{
                flex: 1;
                background: #F0F9FF;
                border: 1px solid #BAE6FD;
                border-radius: 8px;
                padding: 14px;
                text-align: center;
            }}
            .kpi-val {{
                font-size: 24px;
                font-weight: 800;
                color: #0284C7;
            }}
            .kpi-lbl {{
                font-size: 11px;
                color: #475569;
                text-transform: uppercase;
                margin-top: 4px;
            }}
            .footer {{
                margin-top: 40px;
                text-align: center;
                font-size: 11px;
                color: #94A3B8;
                border-top: 1px solid #E2E8F0;
                padding-top: 12px;
            }}
        </style>
    </head>
    <body>
        <div class="cover-bar"></div>
        <div class="title-area">
            <h1 class="main-title">{title}</h1>
            <div class="subtitle">{subtitle}</div>
            <div class="meta-box">
                <span style="margin-right: 24px;"><b>Author:</b> {author}</span>
                <span><b>Date:</b> {date_str}</span>
            </div>
        </div>

        <div class="kpi-row">
            <div class="kpi-card">
                <div class="kpi-val">99.9%</div>
                <div class="kpi-lbl">Render Precision</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-val">&lt; 350ms</div>
                <div class="kpi-lbl">Avg Generation</div>
            </div>
            <div class="kpi-card">
                <div class="kpi-val">Zero-Cloud</div>
                <div class="kpi-lbl">100% On-Device</div>
            </div>
        </div>

        {sections_html}

        <div class="footer">
            PyPrint Studio  |  Chaquopy Native Python Execution Engine  |  Confidential
        </div>
    </body>
    </html>
    """

    result_json_str = generate_pdf_from_html(html_content, "", output_path, engine_preference="auto")
    result = json.loads(result_json_str)
    result["title"] = title
    result["doc_type"] = "REPORT"
    result["source_content"] = html_content
    result["summary"] = f"Executive Report: {title} by {author}"
    return json.dumps(result)


def generate_certificate_pdf(cert_data_json_str, output_path):
    """
    Generates an ornate, elegant Certificate of Achievement or Completion.
    """
    data = json.loads(cert_data_json_str) if isinstance(cert_data_json_str, str) else cert_data_json_str

    recipient = data.get("recipient_name", "Alex M. Henderson")
    title = data.get("title", "CERTIFICATE OF EXCELLENCE")
    subtitle = data.get("subtitle", "For outstanding proficiency in Embedded Mobile Python Systems")
    course = data.get("course_or_achievement", "Advanced Android Architecture & Chaquopy Integration")
    date_str = data.get("date", "September 23, 2026")
    issuer = data.get("issuer", "Python Software Foundation & AI Studio Academy")
    signatory = data.get("signatory", "Dr. Elena Rostova, Dean of Engineering")

    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            @page {{
                size: landscape A4;
                margin: 15mm;
            }}
            body {{
                font-family: Georgia, 'Times New Roman', serif;
                color: #0F172A;
                margin: 0;
                padding: 0;
                background: #FCFCF9;
            }}
            .outer-border {{
                border: 4px solid #0F172A;
                padding: 6px;
                border-radius: 4px;
            }}
            .inner-border {{
                border: 2px solid #D97706;
                padding: 30px 40px;
                text-align: center;
                background: #FFFFFF;
                border-radius: 2px;
            }}
            .gold-badge {{
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                display: inline-block;
                color: #B45309;
                font-weight: 800;
                font-size: 13px;
                letter-spacing: 3px;
                text-transform: uppercase;
                margin-bottom: 8px;
            }}
            .main-title {{
                font-size: 34px;
                font-weight: 800;
                color: #0F172A;
                margin: 4px 0 10px 0;
                letter-spacing: 2px;
                text-transform: uppercase;
            }}
            .certify-text {{
                font-style: italic;
                font-size: 15px;
                color: #475569;
                margin-bottom: 12px;
            }}
            .recipient {{
                font-size: 32px;
                font-weight: 700;
                color: #0369A1;
                border-bottom: 2px solid #E2E8F0;
                display: inline-block;
                padding: 0 40px 6px 40px;
                margin-bottom: 16px;
            }}
            .course-text {{
                font-size: 15px;
                color: #334155;
                max-width: 700px;
                margin: 0 auto 30px auto;
                line-height: 1.6;
            }}
            .sig-row {{
                display: flex;
                justify-content: space-around;
                margin-top: 40px;
                padding-top: 20px;
            }}
            .sig-box {{
                width: 250px;
                text-align: center;
            }}
            .sig-line {{
                border-top: 1px solid #94A3B8;
                margin-bottom: 6px;
            }}
            .sig-name {{
                font-family: -apple-system, sans-serif;
                font-size: 12px;
                font-weight: 700;
                color: #0F172A;
            }}
            .sig-title {{
                font-family: -apple-system, sans-serif;
                font-size: 10px;
                color: #64748B;
            }}
        </style>
    </head>
    <body>
        <div class="outer-border">
            <div class="inner-border">
                <div class="gold-badge">Official Recognition</div>
                <h1 class="main-title">{title}</h1>
                <div class="certify-text">This is proudly presented to</div>
                <div class="recipient">{recipient}</div>
                <div class="course-text">
                    {subtitle}
                    <br>
                    <b>"{course}"</b>
                </div>

                <div class="sig-row">
                    <div class="sig-box">
                        <div style="font-family: -apple-system, sans-serif; font-size: 13px; font-weight: 600; color: #0F172A; margin-bottom: 6px;">{date_str}</div>
                        <div class="sig-line"></div>
                        <div class="sig-name">Date Awarded</div>
                        <div class="sig-title">{issuer}</div>
                    </div>
                    <div class="sig-box">
                        <div style="font-family: 'Brush Script MT', cursive, Georgia; font-size: 18px; color: #0284C7; margin-bottom: 2px;">Elena Rostova</div>
                        <div class="sig-line"></div>
                        <div class="sig-name">{signatory}</div>
                        <div class="sig-title">Authorized Signature</div>
                    </div>
                </div>
            </div>
        </div>
    </body>
    </html>
    """

    result_json_str = generate_pdf_from_html(html_content, "", output_path, engine_preference="auto")
    result = json.loads(result_json_str)
    result["title"] = f"Certificate - {recipient}"
    result["doc_type"] = "CERTIFICATE"
    result["source_content"] = html_content
    result["summary"] = f"Awarded to {recipient} for {course}"
    return json.dumps(result)


def execute_python_code(code_string, output_pdf_path=None):
    """
    Executes arbitrary Python code safely capturing stdout, stderr, and any generated PDF.
    This fulfills the interactive Python runtime inside the Android application.
    """
    start_time = time.time()
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    redirected_output = io.StringIO()
    redirected_error = io.StringIO()
    sys.stdout = redirected_output
    sys.stderr = redirected_error

    try:
        import pdf_generator
        pdf_gen_module = pdf_generator
        convert_fn = pdf_generator.convert_html_to_pdf
    except Exception:
        pdf_gen_module = None
        convert_fn = None

    exec_globals = {
        "__name__": "__main__",
        "output_pdf_path": output_pdf_path,
        "generate_pdf_from_html": generate_pdf_from_html,
        "convert_html_to_pdf": convert_fn or (lambda html, out=output_pdf_path: generate_pdf_from_html(html, "", out)),
        "generate_invoice_pdf": generate_invoice_pdf,
        "generate_report_pdf": generate_report_pdf,
        "generate_certificate_pdf": generate_certificate_pdf,
        "pdf_generator": pdf_gen_module
    }

    success = False
    error_detail = None

    try:
        exec(code_string, exec_globals)
        success = True
    except Exception as e:
        error_detail = traceback.format_exc()
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr

    stdout_val = redirected_output.getvalue()
    stderr_val = redirected_error.getvalue()
    if error_detail:
        stderr_val += "\n" + error_detail

    duration_ms = int((time.time() - start_time) * 1000)
    pdf_created = bool(output_pdf_path and os.path.exists(output_pdf_path) and os.path.getsize(output_pdf_path) > 0)
    pdf_size = os.path.getsize(output_pdf_path) if pdf_created else 0

    return json.dumps({
        "success": success,
        "stdout": stdout_val,
        "stderr": stderr_val,
        "duration_ms": duration_ms,
        "pdf_created": pdf_created,
        "output_pdf_path": output_pdf_path if pdf_created else None,
        "file_size": pdf_size
    })
