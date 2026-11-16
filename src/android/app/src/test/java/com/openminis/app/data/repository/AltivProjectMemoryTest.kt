package com.openminis.app.data.repository

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AltivProjectMemoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun memory(): MemoryRepository = MemoryRepository(temporary.newFolder("memory"))

    @Test fun projectNotesAreIsolatedAndDoNotLeakIntoGlobalMemoryGet() {
        val repo = memory()
        assertTrue(repo.writeProjectMemory("github:team/barber", "Barbershop: menu mobile fixed").startsWith("Project memory saved"))
        assertTrue(repo.writeProjectMemory("github:team/clinic", "Clinic: RLS multiempresa verified").startsWith("Project memory saved"))
        val barber = repo.getProjectMemory("github:team/barber")
        val clinic = repo.getProjectMemory("github:team/clinic")
        assertTrue(barber.contains("menu mobile fixed"))
        assertFalse(barber.contains("Clinic:"))
        assertTrue(clinic.contains("RLS multiempresa"))
        assertFalse(clinic.contains("Barbershop:"))
        assertFalse(repo.getMemory("", "all").contains("Barbershop:"))
        assertFalse(repo.getMemory("", "daily").contains("Clinic:"))
    }

    @Test fun projectKeyNormalizationIsStableAndPathIsHashed() {
        val repo = memory()
        repo.writeProjectMemory("GitHub:TEAM/BARBER", "First visual QA fix")
        assertTrue(repo.getProjectMemory("github:team/barber").contains("First visual QA fix"))
        val files = temporary.root.resolve("memory").listFiles()!!.filter { it.name.startsWith("PROJECT_") }
        assertEquals(1, files.size)
        assertFalse(files.first().name.contains("barber"))
        assertEquals("md", files.first().extension)
    }

    @Test fun invalidProjectKeysAndSecretsAreRejected() {
        val repo = memory()
        assertTrue(repo.writeProjectMemory("../escape\n", "example").startsWith("Error:"))
        assertTrue(repo.writeProjectMemory("github:team/app", "api_key = sk-12345678901234567890").startsWith("Error:"))
        assertTrue(repo.writeProjectMemory("github:team/app", "ghp_12345678901234567890").startsWith("Error:"))
        assertEquals("No saved memory for this project.", repo.getProjectMemory("github:team/app"))
    }

    @Test fun projectRecallIsBoundedAndUserCanDeleteViaExistingMemoryUi() {
        val repo = memory()
        repeat(20) { idx -> repo.writeProjectMemory("local:altive-nexus", "Test ${idx} passed, build verified") }
        val output = repo.getProjectMemory("local:altive-nexus")
        assertTrue(output.length <= 8_000)
        assertTrue(output.contains("Test 19"))
        assertFalse(repo.getProjectMemory("local:another-site").contains("Test"))
        val project = repo.listAllFiles().firstOrNull { it.name.startsWith("PROJECT_") }
        assertNotNull(project)
        assertTrue(repo.deleteFile(project!!.name))
        assertEquals("No saved memory for this project.", repo.getProjectMemory("local:altive-nexus"))
    }

    @Test fun keywordRecallStaysInsideTheRequestedProject() {
        val repo = memory()
        repo.writeProjectMemory("local:project-a", "mobile contrast fixed")
        repo.writeProjectMemory("local:project-b", "mobile contrast fails")
        assertTrue(repo.getProjectMemory("local:project-a", "contrast").contains("fixed"))
        assertFalse(repo.getProjectMemory("local:project-a", "contrast").contains("fails"))
        assertEquals("No matching notes for this project.", repo.getProjectMemory("local:project-a","supabase"))
    }
}
