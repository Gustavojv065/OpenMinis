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

    @Test fun auditDoesNotModifyThePageOrMakeNetworkRequests() {
        val script = AltivVisualAudit.script
        assertFalse(script.contains("fetch("))
        assertFalse(script.contains("XMLHttpRequest"))
        assertFalse(script.contains(".click("))
        assertFalse(script.contains("innerHTML ="))
        assertFalse(script.contains("style.setProperty("))
    }
}
