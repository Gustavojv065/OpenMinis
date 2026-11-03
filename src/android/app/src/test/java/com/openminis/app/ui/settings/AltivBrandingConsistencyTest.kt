package com.openminis.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Static checks that ALTIV branding does not modify the Minis flavor. */
class AltivBrandingConsistencyTest {
    @Test
    fun altivResourceOverlaysUseOwnNameAndFirstRunCopy() {
        val base = File("src/altiv/res/values/strings.xml").readText()
        val pt = File("src/altiv/res/values-pt-rBR/strings.xml").readText()
        for (resources in listOf(base, pt)) {
            assertTrue(resources.contains("""name="app_name">ALTIV DEV AI</string>"""))
            assertTrue(resources.contains("""name="sessionlist_welcome_title""""))
            assertTrue(resources.contains("""name="about_minis_tagline""""))
            assertFalse(resources.contains(">Welcome to Minis<"))
            assertFalse(resources.contains(">Boas-vindas ao Minis<"))
        }
        val upstream = File("src/main/res/values/strings.xml").readText()
        assertTrue(upstream.contains("""name="app_name">Minis</string>"""))
    }

    @Test
    fun altivVariantKeepsItsOwnAdaptiveLauncherIconAndAppId() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("""applicationId = "online.altiv.devai""""))
        val launcher = File("src/altiv/res/mipmap-anydpi-v26/ic_launcher.xml").readText()
        assertTrue(launcher.contains("@drawable/altiv_icon_foreground"))
        assertTrue(launcher.contains("@drawable/altiv_icon_monochrome"))
        val foreground = File("src/altiv/res/drawable/altiv_icon_foreground.xml").readText()
        assertTrue(foreground.contains("ALTIV DEV AI"))
    }

    @Test
    fun welcomeAndSoulDefaultsUseBrandWithoutChangingCustomizedFile() {
        val sessions = File("src/main/java/com/openminis/app/ui/sessions/SessionListScreen.kt").readText()
        val onboarding = File("src/main/java/com/openminis/app/ui/onboarding/OnboardingScreen.kt").readText()
        val about = File("src/main/java/com/openminis/app/ui/settings/AboutScreen.kt").readText()
        val soul = File("src/main/java/com/openminis/app/agent/SoulStore.kt").readText()
        assertTrue(sessions.contains("AppBrandLogo(modifier = Modifier.size(80.dp))"))
        assertTrue(onboarding.contains("AppBrandLogo(modifier = Modifier.size(104.dp))"))
        assertTrue(about.contains("https://github.com/Gustavojv065/OpenMinis"))
        assertTrue(soul.contains("defaultContentFor(context)"))
        assertTrue(soul.contains("file.readText() == DEFAULT_CONTENT"))
        assertTrue(soul.contains("ALTIV DEV AI"))
    }

    @Test
    fun altivSupportUsesOwnLinks() {
        val settings = File("src/main/java/com/openminis/app/ui/settings/SettingsScreen.kt").readText()
        assertTrue(settings.contains("https://wa.me/5565992666940"))
        assertTrue(settings.contains("Gustavojv065/OpenMinis"))
        assertTrue(settings.contains("""if (BuildConfig.FLAVOR == "altiv")"""))
    }
}
