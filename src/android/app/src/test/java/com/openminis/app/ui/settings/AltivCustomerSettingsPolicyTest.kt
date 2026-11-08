package com.openminis.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivCustomerSettingsPolicyTest {
    @Test fun altivCustomerHidesUpstreamAboutAndFeedback() {
        assertTrue(AltivCustomerSettingsPolicy.isAltiv("altiv"))
        assertFalse(AltivCustomerSettingsPolicy.showAboutAndFeedback("altiv"))
    }

    @Test fun upstreamAppPreservesExistingAboutAndFeedback() {
        assertFalse(AltivCustomerSettingsPolicy.isAltiv("github"))
        assertTrue(AltivCustomerSettingsPolicy.showAboutAndFeedback("github"))
    }
}
