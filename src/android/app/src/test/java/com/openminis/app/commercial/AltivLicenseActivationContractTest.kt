package com.openminis.app.commercial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivLicenseActivationContractTest {
    @Test fun acceptsOnlyCanonicalAltivKeys() {
        assertTrue(AltivLicenseActivationContract.acceptsCode(" altiv-abc123-zz999 "))
        assertFalse(AltivLicenseActivationContract.acceptsCode("sk-live-not-a-license"))
        assertFalse(AltivLicenseActivationContract.acceptsCode("ALTIV-"))
        assertFalse(AltivLicenseActivationContract.acceptsCode("https://evil.example"))
        assertEquals("ALTIV-ABC123-ZZ999",
            AltivLicenseActivationContract.normalizeLicenseKey(" altiv-abc123-zz999 "))
    }

    @Test fun doesNotTreatInvalidServerResponseAsSuccess() {
        assertFalse(AltivLicenseActivationContract.isValidActivationResult(
            true, "", "active"))
        assertFalse(AltivLicenseActivationContract.isValidActivationResult(
            false, "token", "active"))
        assertFalse(AltivLicenseActivationContract.isValidActivationResult(
            true, "token", "blocked"))
        assertTrue(AltivLicenseActivationContract.isValidActivationResult(
            true, "token", "active"))
    }

    @Test fun identifiesRateLimitExpiryAndDeviceLimit() {
        assertTrue(AltivLicenseActivationContract.messageForStatus(429).contains("Muitas"))
        assertTrue(AltivLicenseActivationContract.messageForStatus(403).contains("vencida"))
        assertTrue(AltivLicenseActivationContract.messageForStatus(409).contains("dispositivos"))
    }
}
