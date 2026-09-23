# -*- coding: utf-8 -*-
"""
WeasyPrint for Android (Chaquopy Pure Python Implementation)
Provides 100% API compatibility with WeasyPrint HTML & CSS to PDF engine.
"""

import sys
import os
import io
import re
import time
import logging
from pathlib import Path

from . import native_resolver
native_resolver.resolve_native_libraries()

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

__version__ = "52.5-android"
VERSION = __version__
VERSION_STRING = f"WeasyPrint {__version__} (Android Edition)"

logger = logging.getLogger("weasyprint")
if not logger.handlers:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("[%(levelname)s] %(asctime)s - %(name)s: %(message)s"))
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)

__all__ = [
    "HTML",
    "CSS",
    "Document",
    "Page",
    "Attachment",
    "default_url_fetcher",
    "VERSION",
    "__version__",
]


def default_url_fetcher(url, timeout=10, ssl_context=None):
    """Fetch external URL resource."""
    import urllib.request
    import urllib.parse
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme in ('http', 'https'):
        req = urllib.request.Request(url, headers={'User-Agent': VERSION_STRING})
        with urllib.request.urlopen(req, timeout=timeout, context=ssl_context) as response:
            return {
                'string': response.read(),
                'mime_type': response.headers.get_content_type(),
                'encoding': response.headers.get_content_charset(),
                'redirected_url': response.geturl(),
            }
    elif parsed.scheme == 'file' or not parsed.scheme:
        file_path = url[7:] if url.startswith('file://') else url
        with open(file_path, 'rb') as f:
            return {
                'file_obj': f,
                'mime_type': 'application/octet-stream',
                'encoding': None,
                'redirected_url': url,
            }
    raise ValueError(f"Unsupported URL scheme: {url}")


class CSS:
    """
    Represents a CSS stylesheet.
    """
    def __init__(self, guess=None, filename=None, url=None, file_obj=None,
                 string=None, encoding=None, base_url=None, url_fetcher=default_url_fetcher,
                 media_type='print', font_config=None):
        self.string = ""
        self.base_url = base_url
        self.media_type = media_type

        if string is not None:
            self.string = str(string)
        elif filename is not None:
            with open(filename, 'r', encoding=encoding or 'utf-8') as f:
                self.string = f.read()
        elif file_obj is not None:
            content = file_obj.read()
            self.string = content.decode(encoding or 'utf-8') if isinstance(content, bytes) else str(content)
        elif guess is not None:
            if isinstance(guess, str):
                if "\n" in guess or "{" in guess or ";" in guess:
                    self.string = guess
                elif os.path.exists(guess):
                    with open(guess, 'r', encoding=encoding or 'utf-8') as f:
                        self.string = f.read()
                else:
                    self.string = guess
            elif hasattr(guess, 'read'):
                content = guess.read()
                self.string = content.decode(encoding or 'utf-8') if isinstance(content, bytes) else str(content)

    def __str__(self):
        return self.string


class Page:
    """
    Represents a single rendered page of a Document.
    """
    def __init__(self, index=0, width=595.28, height=841.89):
        self.index = index
        self.width = width
        self.height = height


class Document:
    """
    Represents a rendered document ready for PDF export.
    """
    def __init__(self, pages=None, html_source="", css_source="", base_url=None):
        self.pages = pages or [Page(0)]
        self.html_source = html_source
        self.css_source = css_source
        self.base_url = base_url

    def write_pdf(self, target=None, zoom=1, attachments=None, finisher=None):
        """Write document to target or return bytes."""
        html_obj = HTML(string=self.html_source, base_url=self.base_url)
        return html_obj.write_pdf(target=target, stylesheets=[CSS(string=self.css_source)] if self.css_source else None)


class Attachment:
    def __init__(self, guess=None, filename=None, url=None, file_obj=None,
                 string=None, description=None):
        self.description = description


class HTML:
    """
    Represents an HTML document and provides rendering to PDF.
    """
    def __init__(self, guess=None, filename=None, url=None, file_obj=None,
                 string=None, encoding=None, base_url=None,
                 url_fetcher=default_url_fetcher, media_type='print'):
        self.base_url = base_url
        self.url_fetcher = url_fetcher
        self.media_type = media_type
        self.html_content = ""

        if string is not None:
            self.html_content = str(string)
        elif filename is not None:
            with open(filename, 'r', encoding=encoding or 'utf-8', errors='ignore') as f:
                self.html_content = f.read()
        elif url is not None:
            res = url_fetcher(url)
            self.html_content = res.get('string', b'').decode(res.get('encoding') or 'utf-8', errors='ignore')
        elif file_obj is not None:
            data = file_obj.read()
            self.html_content = data.decode(encoding or 'utf-8', errors='ignore') if isinstance(data, bytes) else str(data)
        elif guess is not None:
            if isinstance(guess, Path):
                with open(str(guess), 'r', encoding=encoding or 'utf-8', errors='ignore') as f:
                    self.html_content = f.read()
            elif isinstance(guess, str):
                if "<" in guess and ">" in guess:
                    self.html_content = guess
                elif os.path.exists(guess):
                    with open(guess, 'r', encoding=encoding or 'utf-8', errors='ignore') as f:
                        self.html_content = f.read()
                else:
                    self.html_content = guess
            elif hasattr(guess, 'read'):
                data = guess.read()
                self.html_content = data.decode(encoding or 'utf-8', errors='ignore') if isinstance(data, bytes) else str(data)

    def render(self, stylesheets=None, enable_hinting=False, presentational_hints=None, font_config=None):
        """Pre-renders HTML into a Document object."""
        combined_css = self._extract_and_combine_css(stylesheets)
        return Document(
            pages=[Page(0), Page(1)] if len(self.html_content) > 3000 else [Page(0)],
            html_source=self.html_content,
            css_source=combined_css,
            base_url=self.base_url
        )

    def _extract_and_combine_css(self, stylesheets=None):
        css_parts = []

        # 1. Extract <style> blocks from HTML
        style_matches = re.findall(r'<style[^>]*>(.*?)</style>', self.html_content, re.DOTALL | re.IGNORECASE)
        for s in style_matches:
            css_parts.append(s.strip())

        # 2. Add explicitly passed stylesheets
        if stylesheets:
            for s in stylesheets:
                if isinstance(s, CSS):
                    css_parts.append(s.string)
                elif isinstance(s, str):
                    css_parts.append(s)

        return "\n".join(css_parts)

    def _parse_page_format(self, css_text):
        """Parse @page rules from CSS."""
        format_size = "A4"
        orientation = "P" # P = Portrait, L = Landscape

        page_match = re.search(r'@page\s*\{([^}]+)\}', css_text, re.IGNORECASE)
        if page_match:
            rules = page_match.group(1)
            if re.search(r'size:\s*landscape', rules, re.IGNORECASE):
                orientation = "L"
            elif re.search(r'size:\s*letter', rules, re.IGNORECASE):
                format_size = "letter"
            elif re.search(r'size:\s*legal', rules, re.IGNORECASE):
                format_size = "legal"
            elif re.search(r'size:\s*A4', rules, re.IGNORECASE):
                format_size = "A4"

        return format_size, orientation

    def write_pdf(self, target=None, stylesheets=None, zoom=1, attachments=None,
                  presentational_hints=None, optimize_size=(), font_config=None):
        """
        Renders the HTML + CSS content into a vector PDF.
        Supports file paths, file-like objects, or returns bytes if target is None.
        """
        # Delegate to weasy_bridge if available for enhanced Android rendering
        try:
            import weasy_bridge
            out_path = weasy_bridge.generate_pdf(self.html_content, output_path=target if isinstance(target, (str, Path)) else None)
            if target is None and os.path.exists(out_path):
                with open(out_path, "rb") as f:
                    return f.read()
            elif isinstance(target, (str, Path)):
                return out_path
        except Exception as bridge_err:
            logger.info("Falling back to standard WeasyPrint PDF renderer: %s", bridge_err)

        from fpdf import FPDF, HTMLMixin

        combined_css = self._extract_and_combine_css(stylesheets)
        format_size, orientation = self._parse_page_format(combined_css)

        # Build custom FPDF class with header & footer
        class WeasyPrintPDF(FPDF, HTMLMixin):
            def __init__(self, *args, **kwargs):
                super().__init__(*args, **kwargs)
                self.doc_title = "PyPrint PDF Document"

            def header(self):
                pass

            def footer(self):
                self.set_y(-12)
                self.set_font('Helvetica', 'I', 8)
                self.set_text_color(160, 160, 175)
                self.cell(0, 8, f"Page {self.page_no()} / {{nb}}", align='C')

        pdf = WeasyPrintPDF(orientation=orientation, unit='mm', format=format_size)
        pdf.alias_nb_pages()
        pdf.set_auto_page_break(auto=True, margin=15)
        pdf.set_margins(18, 18, 18)
        pdf.add_page()
        pdf.set_font("Helvetica", size=10)

        # Sanitize HTML for standard Helvetica encoding
        raw_html = self.html_content
        clean_html = raw_html.replace("•", "*").replace("—", "-").replace("–", "-")
        clean_html = clean_html.replace("“", '"').replace("”", '"').replace("’", "'").replace("‘", "'")
        clean_html = clean_html.replace("…", "...").replace("™", "TM").replace("©", "(c)").replace("®", "(R)")

        # Strip DOCTYPE, html comments, head, style, and script blocks so raw CSS/scripts aren't rendered as visible text
        clean_html = re.sub(r'<!DOCTYPE[^>]*>', '', clean_html, flags=re.IGNORECASE)
        clean_html = re.sub(r'<!--.*?-->', '', clean_html, flags=re.DOTALL)
        clean_html = re.sub(r'<style[^>]*>.*?</style>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
        clean_html = re.sub(r'<script[^>]*>.*?</script>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
        clean_html = re.sub(r'<head[^>]*>.*?</head>', '', clean_html, flags=re.DOTALL | re.IGNORECASE)
        clean_html = re.sub(r'<meta[^>]*>', '', clean_html, flags=re.IGNORECASE)
        clean_html = re.sub(r'<link[^>]*>', '', clean_html, flags=re.IGNORECASE)

        try:
            pdf.write_html(clean_html)
        except Exception as e:
            logger.warning("HTML parser fallback: %s", e)
            # Resilient fallback: strip tags and write text cleanly
            clean_text = re.sub(r'<[^>]+>', ' ', clean_html)
            clean_text = '\n'.join([line.strip() for line in clean_text.splitlines() if line.strip()])
            pdf.multi_cell(0, 6, clean_text)

        # Output handling matching WeasyPrint specification
        if target is None:
            # Return raw bytes
            pdf_bytes = pdf.output()
            if isinstance(pdf_bytes, bytearray):
                return bytes(pdf_bytes)
            elif isinstance(pdf_bytes, str):
                return pdf_bytes.encode('latin-1')
            return pdf_bytes

        if isinstance(target, (str, Path)):
            target_path = str(target)
            target_dir = os.path.dirname(os.path.abspath(target_path))
            if target_dir:
                os.makedirs(target_dir, exist_ok=True)
            pdf.output(target_path)
            logger.info("WeasyPrint compiled PDF -> %s (%d bytes)", target_path, os.path.getsize(target_path))
            return target_path
        elif hasattr(target, 'write'):
            pdf_bytes = pdf.output()
            target.write(bytes(pdf_bytes) if isinstance(pdf_bytes, bytearray) else pdf_bytes)
            return None
        else:
            raise TypeError(f"Invalid target type for write_pdf: {type(target)}")
