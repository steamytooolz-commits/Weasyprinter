# -*- coding: utf-8 -*-
"""
PyPrint Dedicated WeasyPrint & HTML PDF Generator
Accepts HTML strings and compiles them directly into PDF files using WeasyPrint.
"""

import sys
import os
import tempfile
import time
import logging
import weasyprint

logger = logging.getLogger("pdf_generator")
if not logger.handlers:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("[%(levelname)s] %(asctime)s - %(name)s: %(message)s"))
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)


def generate_pdf_from_html(html_string, output_pdf_path=None, css_string=None, base_url=None):
    """
    Renders an HTML string into a PDF file using WeasyPrint.

    Args:
        html_string (str): The HTML markup to convert into PDF.
        output_pdf_path (str, optional): Target PDF destination path.
        css_string (str, optional): Additional CSS stylesheet string.
        base_url (str, optional): Base URL for resolving relative assets.

    Returns:
        str: Absolute path of the generated PDF file.
    """
    if not html_string or not html_string.strip():
        raise ValueError("html_string cannot be empty")

    if not output_pdf_path:
        timestamp = int(time.time() * 1000)
        temp_dir = tempfile.gettempdir()
        output_pdf_path = os.path.join(temp_dir, f"weasyprint_doc_{timestamp}.pdf")

    target_dir = os.path.dirname(os.path.abspath(output_pdf_path))
    if target_dir:
        os.makedirs(target_dir, exist_ok=True)

    stylesheets = [weasyprint.CSS(string=css_string)] if css_string else None
    html_doc = weasyprint.HTML(string=html_string, base_url=base_url)
    html_doc.write_pdf(target=output_pdf_path, stylesheets=stylesheets)

    if os.path.exists(output_pdf_path) and os.path.getsize(output_pdf_path) > 0:
        logger.info("PDF compiled successfully -> %s (%d bytes)", output_pdf_path, os.path.getsize(output_pdf_path))
        return os.path.abspath(output_pdf_path)
    else:
        raise RuntimeError(f"Failed to generate PDF at {output_pdf_path}")


def convert_html_to_pdf(html_string, output_pdf_path=None):
    """
    Convenience alias for generate_pdf_from_html.
    """
    return generate_pdf_from_html(html_string=html_string, output_pdf_path=output_pdf_path)


if __name__ == "__main__":
    test_html = "<h1>PyPrint WeasyPrint</h1><p>Test document generation on Android.</p>"
    out = convert_html_to_pdf(test_html)
    print("Test output PDF:", out)
