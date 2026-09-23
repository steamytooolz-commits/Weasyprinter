package com.example

import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTemplateValidationTest {

    @Test
    fun testInvoiceTemplateContainsValidWeasyPrintCss() {
        val html = MainViewModel.DEFAULT_HTML_TEMPLATE
        val css = MainViewModel.DEFAULT_CSS_TEMPLATE

        // Verify HTML tags
        assertTrue("HTML should have invoice-box", html.contains("invoice-box"))
        assertTrue("HTML should have client details", html.contains("Billed to"))
        assertTrue("HTML should have items table", html.contains("<table"))

        // Verify CSS @page layout rules for WeasyPrint paged media
        assertTrue("CSS should specify @page size", css.contains("@page"))
        assertTrue("CSS should specify margin", css.contains("margin"))
        assertTrue("CSS should style table header", css.contains("th"))
    }

    @Test
    fun testBadgeTemplateContainsValidDimensions() {
        val html = MainViewModel.BADGE_HTML_TEMPLATE
        val css = MainViewModel.BADGE_CSS_TEMPLATE

        assertTrue("Badge should have badge container", html.contains("badge"))
        assertTrue("Badge should have attendee name", html.contains("SARAH CHEN") || html.contains("attendee-name"))

        // Check @page dimensions suitable for badge / ID card
        assertTrue("CSS must configure @page", css.contains("@page"))
        assertTrue("CSS must define size", css.contains("size:"))
    }

    @Test
    fun testReceiptTemplateContainsPosStyling() {
        val html = MainViewModel.RECEIPT_HTML_TEMPLATE
        val css = MainViewModel.RECEIPT_CSS_TEMPLATE

        assertTrue(html.contains("receipt"))
        assertTrue(html.contains("CAFE") || html.contains("TOTAL"))

        // POS receipt uses compact continuous page or small width
        assertTrue("Receipt CSS should contain font or size styling", css.contains("font-family") || css.contains("width"))
    }

    @Test
    fun testLetterTemplateContainsLetterhead() {
        val html = MainViewModel.LETTER_HTML_TEMPLATE
        val css = MainViewModel.LETTER_CSS_TEMPLATE

        assertTrue(html.contains("letterhead"))
        assertTrue(html.contains("Sincerely"))

        assertTrue(css.contains("@page"))
        assertTrue(css.contains("letter") || css.contains("margin"))
    }

    @Test
    fun testPythonFpdf2TemplateIsValid() {
        val script = MainViewModel.FPDF2_EXAMPLE_SCRIPT

        assertTrue("Script should import FPDF", script.contains("from fpdf import FPDF"))
        assertTrue("Script should add page", script.contains("add_page()"))
        assertTrue("Script should write text", script.contains("pdf.cell"))
        assertTrue("Script should handle output_pdf_path", script.contains("output_pdf_path"))
    }
}
