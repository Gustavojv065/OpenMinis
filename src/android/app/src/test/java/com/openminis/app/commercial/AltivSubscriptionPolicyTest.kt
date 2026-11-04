package com.openminis.app.commercial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivSubscriptionPolicyTest {
    private fun entry(status: String, active: Boolean) =
        AltivSubscription(active, "monthly", status, "2026-12-05T00:00:00Z", "2026-11-04T00:00:00Z")

    @Test fun onlyVerifiedActiveCanUseHostedPaidServices() {
        assertTrue(AltivSubscriptionPolicy.allowsHostedServices(entry("active", true)))
        assertFalse(AltivSubscriptionPolicy.allowsHostedServices(entry("active", false)))
        assertFalse(AltivSubscriptionPolicy.allowsHostedServices(entry("suspended", true)))
        assertFalse(AltivSubscriptionPolicy.allowsHostedServices(entry("cancelled", true)))
        assertFalse(AltivSubscriptionPolicy.allowsHostedServices(entry("pending", true)))
        assertFalse(AltivSubscriptionPolicy.allowsHostedServices(null))
    }

    @Test fun statusNeverConvertsAnUnverifiedSessionIntoActive() {
        assertEquals("not_found", AltivSubscriptionPolicy.safeStatus(null))
        assertEquals("expired", AltivSubscriptionPolicy.safeStatus(entry("active", false)))
        assertEquals("suspended", AltivSubscriptionPolicy.safeStatus(entry("suspended", false)))
        assertEquals("active", AltivSubscriptionPolicy.safeStatus(entry("active", true)))
    }

    @Test fun commercialIsOnlyEnabledOnAltivFlavor() {
        val main = File("src/main/java/com/openminis/app/MainActivity.kt").readText()
        assertTrue(main.contains("if (BuildConfig.FLAVOR == \"altiv\")"))
        assertTrue(main.contains("AltivCommercialEntry"))
        assertTrue(main.contains("appNavigationContent()"))
        val build = File("build.gradle.kts").readText()
        assertTrue(build.contains("ALTIV_COMMERCIAL_URL"))
        assertTrue(build.contains("ALTIV_COMMERCIAL_PUBLISHABLE_KEY"))
        assertFalse(build.contains("service_role"))
    }

    @Test fun commercialValidationUsesServerClockAndOwnRowRls() {
        val sql = File("../../../commercial/sql/altiv_subscriptions_v1.sql").readText()
        assertTrue(sql.contains("enable row level security"))
        assertTrue(sql.contains("using ((select auth.uid()) = user_id)"))
        assertTrue(sql.contains("security invoker"))
        assertTrue(sql.contains("now()"))
        assertTrue(sql.contains("revoke all on public.altiv_service_subscriptions"))
        assertFalse(sql.contains("security definer"))
        assertFalse(sql.contains("grant update"))
    }

    @Test fun tokensAreEncryptedAndNeverStoredInPlaintextPrefs() {
        val store = File("src/main/java/com/openminis/app/commercial/AltivCommercialSessionStore.kt").readText()
        assertTrue(store.contains("AndroidKeyStore"))
        assertTrue(store.contains("AES/GCM/NoPadding"))
        assertTrue(store.contains("Base64.encodeToString(packed"))
        assertFalse(store.contains("putString(\"access_token\""))
    }
}
