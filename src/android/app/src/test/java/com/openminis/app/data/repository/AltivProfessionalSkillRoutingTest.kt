package com.openminis.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivProfessionalSkillRoutingTest {
    @Test fun frontendAnd3dSkillsHaveHigherPriorityThanGenericOnes() {
        assertTrue(
            AltivProfessionalSkillRouting.priority(
                "3D Website Creator", "React Three.js effects"
            ) > AltivProfessionalSkillRouting.priority("General helper", "General writing")
        )
        assertTrue(
            AltivProfessionalSkillRouting.priority(
                "Frontend Design Premium", "Responsive UI UX and TypeScript"
            ) > 0
        )
        assertEquals(0, AltivProfessionalSkillRouting.priority("Cooking", "Recipes"))
    }

    @Test fun codingGuidancePreservesExistingRepoAndRequiresTests() {
        val guidance = AltivProfessionalSkillRouting.GUIDANCE
        assertTrue(guidance.contains("SKILL.md"))
        assertTrue(guidance.contains("typecheck"))
        assertTrue(guidance.contains("without the user's authorization"))
        assertTrue(guidance.contains("repeatedly asking"))
    }
}
