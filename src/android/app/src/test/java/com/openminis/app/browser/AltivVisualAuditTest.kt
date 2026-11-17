package com.openminis.app.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivVisualAuditTest {
    @Test fun auditIsAdvertisedAsARealBrowserAction() {
        assertEquals(BrowserAction.VISUAL_AUDIT, BrowserAction.fromString("visual_audit"))
        assertTrue(BrowserAction.allValues.contains("visual_audit"))
        assertFalse(BrowserAction.visualChangeActions.contains(BrowserAction.VISUAL_AUDIT))
        assertFalse(BrowserAction.VISUAL_AUDIT.opensNewPage)
    }

    @Test fun auditMeasuresCommonMobileRegressionsWithoutClaimingVisualPass() {
        val script = AltivVisualAudit.script
        assertTrue(script.contains("documentWidth"))
        assertTrue(script.contains("brokenImages"))
        assertTrue(script.contains("hiddenSections"))
        assertTrue(script.contains("smallTargets"))
        assertTrue(script.contains("lowContrast"))
        assertTrue(script.contains("NOT_VERIFIED"))
        assertTrue(script.contains("screenshotRequired:true"))
        assertTrue(script.contains("read-only DOM heuristics"))
    }

    @Test fun unavailableVisualFallbacksCannotBecomeSuccessfulImageQa() {
        val script = AltivVisualAudit.script
        assertTrue(script.contains("assetPlaceholders:0"))
        assertTrue(script.contains("visible missing-media fallback"))
        assertTrue(script.contains("indispon[ií]vel"))
        assertTrue(script.contains("sem[ ]+imagem"))
        assertTrue(script.contains("semanticImageApproval:\"NOT_VERIFIED\""))
        assertTrue(script.contains("screenshotRequired:true"))
        assertFalse(script.contains("visualApproval:\"PASS\""))
    }

    @Test fun auditDoesNotModifyThePageOrMakeNetworkRequests() {
        val script = AltivVisualAudit.script
        assertFalse(script.contains("fetch("))
        assertFalse(script.contains("XMLHttpRequest"))
        assertFalse(script.contains(".click("))
        assertFalse(script.contains("innerHTML ="))
        assertFalse(script.contains("style.setProperty("))
    }
}
