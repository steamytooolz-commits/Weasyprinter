# -*- coding: utf-8 -*-
"""
PyPrint Dedicated WeasyPrint & HTML PDF Generator for Android
Implements generate_pdf(html_content, output_path) using WeasyPrint.
Ensures standard library paths for Android compatibility and graceful error handling.
"""

import sys
import os
import io
import tempfile
import time
import logging
from pathlib import Path

# Configure logging
logger = logging.getLogger("pdf_generator")
if not logger.handlers:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("[%(levelname)s] %(asctime)s - %(name)s: %(message)s"))
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)

# Import weasybridge compatibility layer
try:
    import weasy_bridge
except ImportError:
    weasy_bridge = None

try:
    import weasyprint
except ImportError as e:
    logger.error("Failed to import weasyprint: %s", e)
    weasyprint = None


def generate_pdf(html_content: str, output_path: str = None) -> str:
    """
    Generates a PDF document from HTML content using WeasyPrint.

    Args:
        html_content (str): The HTML and inline CSS markup string to convert.
        output_path (str, optional): Target file path for the output PDF.
                                     If omitted, a standard temporary path is used.

    Returns:
        str: The absolute path to the successfully generated PDF file.

    Raises:
        ValueError: If html_content is invalid or empty.
        RuntimeError: If WeasyPrint fails or output file cannot be created.
    """
    if html_content is None or not str(html_content).strip():
        logger.error("generate_pdf: html_content must not be empty.")
        raise ValueError("html_content cannot be empty")

    # Resolve output path using Android-compatible standard library paths
    if not output_path or not str(output_path).strip():
        timestamp = int(time.time() * 1000)
        temp_dir = tempfile.gettempdir()
        resolved_path = os.path.join(temp_dir, f"weasyprint_doc_{timestamp}.pdf")
    else:
        resolved_path = os.path.abspath(str(output_path))

    # Ensure parent directory exists
    try:
        parent_dir = os.path.dirname(resolved_path)
        if parent_dir:
            os.makedirs(parent_dir, exist_ok=True)
    except Exception as e:
        logger.error("Failed to create parent directory for %s: %s", resolved_path, e)
        raise RuntimeError(f"Unable to create destination directory: {e}")

    # Generate PDF using WeasyBridge / WeasyPrint with graceful error handling
    try:
        if weasy_bridge is not None:
            logger.info("Rendering PDF using WeasyBridge -> target: %s", resolved_path)
            res = weasy_bridge.generate_pdf(html_content, output_path=resolved_path)
            if os.path.exists(res) and os.path.getsize(res) > 0:
                logger.info("PDF generated successfully via WeasyBridge: %s (%d bytes)", res, os.path.getsize(res))
                return res

        if weasyprint is None:
            raise RuntimeError("WeasyPrint / WeasyBridge module is not available in the current Python runtime.")

        logger.info("Rendering PDF using WeasyPrint -> target: %s", resolved_path)
        html_doc = weasyprint.HTML(string=str(html_content))
        html_doc.write_pdf(target=resolved_path)

        # Validate that the file was created and is non-empty
        if os.path.exists(resolved_path) and os.path.getsize(resolved_path) > 0:
            file_size = os.path.getsize(resolved_path)
            logger.info("PDF generated successfully: %s (%d bytes)", resolved_path, file_size)
            return resolved_path
        else:
            raise RuntimeError(f"Output PDF was not generated or is empty at {resolved_path}")

    except Exception as e:
        logger.error("Error during WeasyPrint PDF generation: %s", e, exc_info=True)
        # Attempt fallback to bytes stream write if direct file path failed
        try:
            logger.info("Attempting in-memory byte buffer write fallback...")
            pdf_bytes = weasyprint.HTML(string=str(html_content)).write_pdf()
            if pdf_bytes:
                with open(resolved_path, "wb") as f:
                    f.write(pdf_bytes)
                if os.path.exists(resolved_path) and os.path.getsize(resolved_path) > 0:
                    logger.info("Fallback write successful: %s (%d bytes)", resolved_path, os.path.getsize(resolved_path))
                    return resolved_path
        except Exception as fallback_err:
            logger.error("Fallback buffer write also failed: %s", fallback_err)

        raise RuntimeError(f"PDF generation failed: {e}")


def generate_pdf_from_html(html_string: str, output_pdf_path: str = None, css_string: str = None, base_url: str = None) -> str:
    """
    Extended compatibility wrapper for generate_pdf.
    """
    if css_string:
        combined_html = f"<style>{css_string}</style>\n{html_string}"
    else:
        combined_html = html_string
    return generate_pdf(html_content=combined_html, output_path=output_pdf_path)


def convert_html_to_pdf(html_string: str, output_pdf_path: str = None) -> str:
    """
    Convenience alias for generate_pdf.
    """
    return generate_pdf(html_content=html_string, output_path=output_pdf_path)


if __name__ == "__main__":
    sample_html = """
    <!DOCTYPE html>
    <html>
    <head>
      <style>
        @page { size: A4; margin: 20mm; }
        body { font-family: sans-serif; color: #1e293b; }
        h1 { color: #0284c7; border-bottom: 2px solid #0284c7; padding-bottom: 8px; }
        p { font-size: 14px; line-height: 1.6; }
      </style>
    </head>
    <body>
      <h1>WeasyPrint Android Engine</h1>
      <p>This document was generated using pdf_generator.py on Android.</p>
    </body>
    </html>
    """
    try:
        pdf_out = generate_pdf(sample_html)
        print("Generated PDF at:", pdf_out)
    except Exception as err:
        print("Error during test run:", err)
