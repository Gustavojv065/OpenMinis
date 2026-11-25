package com.openminis.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivBundledSkillsTest {
    @Test fun allOriginalAltivSkillsHaveValidDistinctMetadata() {
        assertEquals(13, AltivBundledSkills.contents.size)
        val names = AltivBundledSkills.contents.map { raw ->
            val parsed = SkillRepository.parseSkillMd(raw)
            assertNotNull("Bundled skill must have SKILL.md frontmatter", parsed)
            assertTrue(parsed!!.name.startsWith("altive-"))
            assertEquals(if (parsed.name == "altive-github-safe-workflow") "1.2.0" else "1.0.0", parsed.version)
            assertFalse(parsed.description.isBlank())
            assertFalse(parsed.body.isBlank())
            parsed.name
        }
        assertEquals(names.size, names.distinct().size)
        assertTrue(names.contains("altive-prompt-studio"))
        assertTrue(names.contains("altive-github-safe-workflow"))
        assertTrue(names.contains("altive-supabase-saas-security"))
        assertTrue(names.contains("altive-licensed-media-research"))
        assertTrue(names.contains("altive-template-creator"))
    }

    @Test fun skillsDoNotClaimThirdPartyPluginAccessOrSimulatedSuccess() {
        val text = AltivBundledSkills.contents.joinToString("\n").lowercase()
        assertTrue(text.contains("não declare"))
        assertTrue(text.contains("whatsapp"))
        assertTrue(text.contains("licença"))
        assertTrue(text.contains("build"))
        assertTrue(text.contains("backend"))
        assertTrue(text.contains("390px"))
        assertTrue(text.contains("3d"))
    }

    @Test fun repositoryAutoInstallsCommercialOriginalSkillsWithoutReplacingUsers() {
        val src = java.io.File("src/main/java/com/openminis/app/data/repository/SkillRepository.kt").readText()
        assertTrue(src.contains("installAltivBundledSkills()"))
        assertTrue(src.contains("if (BuildConfig.FLAVOR != \"altiv\") return"))
        assertTrue(src.contains("_skills.value.any { it.id == id }"))
        assertTrue(src.contains("source = ImportSource.BUNDLED"))
    }
}
