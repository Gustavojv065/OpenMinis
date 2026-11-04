package com.openminis.app.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Release regression guards. Android resolves launcher resources by density
 * and night mode; checking only mipmap-anydpi-v26 misses Samsung's old logo.
 * Technical identifiers such as /var/minis and minis-mcp-cli remain unchanged.
 */
class AltivFullBrandingTest {
    private fun appFile(path: String) = File(path).readText()

    @Test fun altivLauncherUsesOwnLogoOnAllAndroidThemesAndDensities() {
        val path = "src/altiv/res"
        val expected = listOf(
            "mipmap-anydpi-v26/ic_launcher.xml",
            "mipmap-anydpi-v26/ic_launcher_classic_light.xml",
            "mipmap-anydpi-v26/ic_launcher_classic_dark.xml",
            "mipmap-night-anydpi-v26/ic_launcher.xml",
            "mipmap-night-anydpi-v26/ic_launcher_classic_light.xml",
            "mipmap-night-anydpi-v26/ic_launcher_classic_dark.xml",
        )
        expected.forEach {
            val icon = appFile("$path/$it")
            assertTrue(it, icon.contains("@drawable/altiv_icon_foreground"))
            assertFalse(it, icon.contains("@mipmap/ic_launcher_foreground"))
        }
        for (mode in listOf("mipmap", "mipmap-night")) {
            for (dpi in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
                val xml = appFile("$path/$mode-$dpi/ic_launcher.xml")
                assertTrue("$mode-$dpi", xml.contains("@drawable/altiv_icon_foreground"))
                assertTrue("$mode-$dpi", xml.contains("@color/altiv_icon_background"))
            }
        }
    }

    @Test fun altivMcpScreenDoesNotDisplayUpstreamToolOrFilesystemDetails() {
        for (locale in listOf("values", "values-pt-rBR")) {
            val xml = appFile("src/altiv/res/$locale/strings.xml")
            val footer = Regex("""<string name="mcp_section_footer">([\\s\\S]*?)</string>""")
                .find(xml)?.groupValues?.get(1)
            assertTrue(locale, footer?.contains("ALTIV DEV AI") == true)
            assertFalse(locale, footer.orEmpty().contains("minis-mcp-cli"))
            assertFalse(locale, footer.orEmpty().contains("/var/minis/"))
            assertTrue(locale, xml.contains("""name="app_name">ALTIV DEV AI</string>"""))
        }
    }

    @Test fun originalMinisStillHasOwnIdentityAndTechnicalPathsRemainIntact() {
        val original = appFile("src/main/res/values/strings.xml")
        assertTrue(original.contains("""name="app_name">Minis</string>"""))
        val mcp = appFile("src/main/java/com/openminis/app/ui/settings/MCPIntegrationsScreen.kt")
        assertTrue(mcp.contains("MCPRepository"))
        val gradle = appFile("build.gradle.kts")
        assertTrue(gradle.contains("""applicationId = "online.altiv.devai""""))
    }
}
