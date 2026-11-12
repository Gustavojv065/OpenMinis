package com.openminis.app.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivHtmlProjectPreviewTest {
    private val complete = """
        <!DOCTYPE html>
        <html lang="pt-br"><head><meta charset="utf-8"></head>
        <body><h1>Mundo Pet</h1><img src="https://example.org/cat.jpg"></body></html>
    """.trimIndent()

    @Test fun onlyCompleteHtmlCanCreateProject() {
        assertTrue(AltivHtmlProjectPreview.isHtmlCandidate("html", complete))
        assertTrue(AltivHtmlProjectPreview.isComplete("html", complete))
        assertFalse(AltivHtmlProjectPreview.isComplete("html", "<header>Começo"))
        assertFalse(AltivHtmlProjectPreview.isComplete(
            "html", "<html><head></head><body>truncado"
        ))
        assertFalse(AltivHtmlProjectPreview.isComplete("javascript", "console.log('ok')"))
    }

    @Test fun previewPolicyDisablesScriptsAndKeepsImagesOnly() {
        val locked = AltivHtmlProjectPreview.lockedPreviewHtml(complete)
        assertTrue(locked.contains("script-src 'none'"))
        assertTrue(locked.contains("img-src https: data:"))
        assertTrue(locked.contains("connect-src 'none'"))
        assertTrue(locked.contains("default-src 'none'"))
    }
}
