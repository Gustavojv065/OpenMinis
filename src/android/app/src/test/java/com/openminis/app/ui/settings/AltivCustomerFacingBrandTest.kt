package com.openminis.app.ui.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ALTIVE-only customer-facing branding regression guard.
 * The shared upstream flavor, package IDs, CLI names and /var/minis paths
 * intentionally remain compatible with previously exported backups.
 */
class AltivCustomerFacingBrandTest {
    private fun source(path: String): String = File(path).readText()

    @Test fun everyLegacyResourceLabelHasAnAltiveOverlayInEnglishAndPortuguese() {
        val re = Regex("""<string name="([^"]+)"[^>]*>([\s\S]*?)</string>""")
        for (locale in listOf("values", "values-pt-rBR")) {
            val original = source("src/main/res/$locale/strings.xml")
            val overlay = source("src/altiv/res/$locale/strings.xml")
            val branded = re.findAll(overlay).associate { it.groupValues[1] to it.groupValues[2] }
            val brandedKeys = branded.keys
            val legacy = re.findAll(original)
                .filter { Regex("""\bMinis\b""").containsMatchIn(it.groupValues[2]) }
                .map { it.groupValues[1] }.toList()
            for (key in legacy) {
                assertTrue("Missing ALTIVE override for $key ($locale)", key in brandedKeys)
            }
            for ((key, value) in branded) {
                assertFalse("ALTIVE value still says Minis ($key/$locale)",
                    Regex("""\bMinis\b""").containsMatchIn(value))
            }
            assertTrue(branded["app_name"]?.contains("ALTIV DEV AI") == true)
        }
    }

    @Test fun quickTestsUseTheFlavorAppBrandInsteadOfUpstreamName() {
        val s = source("src/main/java/com/openminis/app/ui/components/QuickTestSheet.kt")
        assertTrue(s.contains("context.getString(R.string.app_name)"))
        assertFalse(s.contains("This is Minis testing"))
        assertFalse(s.contains("from Minis, testing"))
        assertFalse(s.contains("setting you up in Minis"))
        assertFalse(s.contains("app called Minis"))
    }

    @Test fun filesAndTerminalExposeTheBrandedNameWithoutChangingStorageIds() {
        val files = source("src/main/java/com/openminis/app/providers/MinisDocumentsProvider.kt")
        val terminal = source("src/main/java/com/openminis/app/ui/terminal/TerminalScreen.kt")
        assertTrue(files.contains(".add(Root.COLUMN_TITLE, brandedRootName())"))
        assertTrue(files.contains("brandedRootName() else file.name"))
        assertFalse(files.contains("Root.COLUMN_TITLE, \"Minis\""))
        assertTrue(files.contains("com.openminis.minis.documents"))
        assertTrue(terminal.contains("context.getString(R.string.app_name)"))
        assertFalse(terminal.contains("newPlainText(\"Minis Shell\""))
    }

    @Test fun emptySoulNameUsesAltiveIdentityButPreservesUserCustomizations() {
        val s = source("src/main/java/com/openminis/app/agent/SoulStore.kt")
        assertTrue(s.contains("val defaultName = if (context.packageName == \"online.altiv.devai\")"))
        assertTrue(s.contains(".ifEmpty { defaultName }"))
        assertTrue(s.contains("file?.metadata?.name ?: defaultName"))
        assertTrue(s.contains("file.readText() == DEFAULT_CONTENT"))
    }
}
