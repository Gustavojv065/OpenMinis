package com.openminis.app.tools

import com.openminis.app.data.repository.MemoryRepository
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Regression: PR #45 had correct repository/executor but the authoritative
 * AgentTools provider schema still exposed only daily/all. The model could not
 * submit scope=project or project_key even with a fully implemented backend.
 */
class AltivMemoryToolContractTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun tools() = AgentTools.makeAgentTools(memoryEnabled = true)
    private fun tool(name: String) = tools().first { it.name == name }

    @Test fun projectParametersActuallyReachTheCanonicalToolRegistry() {
        val get = tool("memory_get")
        val write = tool("memory_write")
        assertEquals(listOf("project", "daily", "all"), get.parameters["scope"]!!.enumValues)
        assertTrue("project_key missing from memory_get", get.parameters.containsKey("project_key"))
        assertTrue("project_key missing from memory_write", write.parameters.containsKey("project_key"))
        assertTrue(get.description.contains("MUST NOT fall back"))
        assertTrue(write.description.contains("project_key"))
        assertEquals(listOf("tool_title","scope","project_key","keywords"), get.propertyOrdering)
        assertEquals(listOf("tool_title","content","project_key"), write.propertyOrdering)
    }

    @Test fun everyProviderReceivesProjectParametersOnTheWire() {
        for (name in listOf("memory_get", "memory_write")) {
            val def = tool(name)
            val anthropic = def.toAnthropicJson().getJSONObject("input_schema").getJSONObject("properties")
            val openAi = def.toOpenAIJson().getJSONObject("function")
                .getJSONObject("parameters").getJSONObject("properties")
            val gemini = def.toGeminiJson().getJSONObject("parameters")
                .getJSONObject("properties")
            for (props in listOf(anthropic, openAi, gemini)) {
                assertTrue("$name missing project_key on wire", props.has("project_key"))
            }
            if (name == "memory_get") {
                for (props in listOf(anthropic, openAi, gemini)) {
                    val scope = props.getJSONObject("scope").getJSONArray("enum")
                    assertTrue((0 until scope.length()).any { scope.getString(it) == "project" })
                }
            }
        }
    }

    @Test fun trueToolExecutionStoresPerProjectAndNeverLeaksThroughDaily() {
        val repo = MemoryRepository(temporary.newFolder("isolated"))
        val savedA = MemoryTools.executeMemoryWrite(
            """{"tool_title":"Save NEXUS","project_key":"local:altive-nexus","content":"NEXUS uses premium emerald theme"}""", repo)
        val savedB = MemoryTools.executeMemoryWrite(
            """{"tool_title":"Save Barber","project_key":"local:barbearia-crs","content":"Barber uses real establishment photos"}""", repo)
        assertTrue(savedA.output, savedA.success)
        assertTrue(savedB.output, savedB.success)
        val getA = MemoryTools.executeMemoryGet(
            """{"tool_title":"Recall NEXUS","scope":"project","project_key":"local:altive-nexus"}""", repo)
        val getB = MemoryTools.executeMemoryGet(
            """{"tool_title":"Recall Barber","scope":"project","project_key":"local:barbearia-crs"}""", repo)
        assertTrue(getA.output, getA.success)
        assertTrue(getB.output, getB.success)
        assertTrue(getA.output.contains("emerald theme"))
        assertFalse(getA.output.contains("establishment photos"))
        assertTrue(getB.output.contains("establishment photos"))
        assertFalse(getB.output.contains("emerald theme"))
        val daily = MemoryTools.executeMemoryGet(
            """{"tool_title":"Daily","scope":"daily","keywords":"emerald"}""", repo)
        assertFalse(daily.output.contains("NEXUS uses"))
        val missingKey = MemoryTools.executeMemoryGet(
            """{"tool_title":"Invalid","scope":"project"}""", repo)
        assertFalse(missingKey.success)
        assertTrue(missingKey.output.contains("project_key"))
    }

    @Test fun memoryToggleAndHelperGateRemainUnchanged() {
        assertFalse(AgentTools.makeAgentTools(memoryEnabled = false)
            .any { it.name == "memory_get" || it.name == "memory_write" })
        val helper = AgentTools.makeAgentTools(memoryEnabled = true, isHelper = true)
        assertTrue(helper.any { it.name == "memory_get" })
        assertFalse(helper.any { it.name == "memory_write" })
    }

    @Test fun oldSeparateJsonDefinitionsStayConsistentWithCanonicalSchema() {
        for (name in listOf("memory_get", "memory_write")) {
            val canonical = tool(name).toAnthropicJson().getJSONObject("input_schema")
                .getJSONObject("properties")
            val legacy = (if (name == "memory_get") MemoryTools.memoryGetToolDefinition()
                else MemoryTools.memoryWriteToolDefinition())
                .getJSONObject("input_schema").getJSONObject("properties")
            assertEquals(canonical.keySet(), legacy.keySet())
        }
    }
}
