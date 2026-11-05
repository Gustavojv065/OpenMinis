package com.openminis.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AltivSkillBundleArchiveTest {
    private fun zip(entries: Map<String, ByteArray>): ByteArray {
        val result = ByteArrayOutputStream()
        ZipOutputStream(result).use { out ->
            entries.forEach { (name, body) ->
                out.putNextEntry(ZipEntry(name))
                out.write(body)
                out.closeEntry()
            }
        }
        return result.toByteArray()
    }

    private fun skill(name: String): ByteArray = zip(
        mapOf(
            "SKILL.md" to (
                "---\nname: " + name + "\ndescription: Demo skill\n---\n\n# " + name
            ).toByteArray(),
            "references/README.md" to "Safe reference".toByteArray()
        )
    )

    @Test fun nestedSkillBundlesImportAsOneCollection() {
        val outer = zip(mapOf(
            "00-LEIA-PRIMEIRO.txt" to "Instructions only".toByteArray(),
            "IMPORTAR-UM-POR-VEZ/altiv-3d-motion.zip" to skill("altiv-3d-motion"),
            "IMPORTAR-UM-POR-VEZ/altiv-backend-api.zip" to skill("altiv-backend-api"),
        ))
        val packages = AltivSkillBundleArchive.collect(outer.inputStream())
        assertNotNull(packages)
        assertEquals(2, packages!!.size)
        assertTrue(packages.all { AltivSkillBundleArchive.safeInnerArchive(it.archive) })
    }

    @Test fun singleSkillArchivesStillUseLegacyImporter() {
        assertNull(AltivSkillBundleArchive.collect(skill("altiv-pets").inputStream()))
    }

    @Test fun invalidInnerArchiveIsRejected() {
        val malicious = zip(mapOf(
            "SKILL.md" to "---\nname: evil\n---\n".toByteArray(),
            "../outside.txt" to "no".toByteArray(),
        ))
        assertFalse(AltivSkillBundleArchive.safeInnerArchive(malicious))
    }

    @Test fun overlyLargeZipMemberCannotExhaustMemory() {
        val huge = zip(mapOf(
            "IMPORTAR-UM-POR-VEZ/a.zip" to ByteArray(2 * 1024 * 1024 + 1),
            "IMPORTAR-UM-POR-VEZ/b.zip" to skill("b"),
        ))
        var blocked = false
        try {
            AltivSkillBundleArchive.collect(huge.inputStream())
        } catch (_: IllegalArgumentException) {
            blocked = true
        }
        assertTrue(blocked)
    }
}
