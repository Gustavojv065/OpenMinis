package com.openminis.app.mcp

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivGitHubProjectInspectorTest {
    @Test fun findsOnlyEvidencePresentInTheAuthorizedRepository() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) // Lovable
            server.enqueue(MockResponse().setResponseCode(404)) // Vercel
            server.enqueue(MockResponse().setResponseCode(404)) // Vercel hidden
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}")) // Supabase
            val result = AltivGitHubProjectInspector(apiBase = server.url("/").toString())
                .inspect("secret-token", "user/project")
            assertTrue(result.lovable)
            assertFalse(result.vercel)
            assertTrue(result.supabase)
            assertEquals(2, result.labels().size)
            assertEquals("/repos/user/project/contents/.lovable/project.json", server.takeRequest().path)
            assertEquals("/repos/user/project/contents/vercel.json", server.takeRequest().path)
            assertEquals("/repos/user/project/contents/.vercel/project.json", server.takeRequest().path)
            val supabase = server.takeRequest()
            assertEquals("/repos/user/project/contents/supabase/config.toml", supabase.path)
            assertEquals("Bearer secret-token", supabase.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun detectsVercelWithoutReadingPrivateFileContents() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404))
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            server.enqueue(MockResponse().setResponseCode(404))
            val signals = AltivGitHubProjectInspector(apiBase = server.url("/").toString())
                .inspect("token", "owner/repo")
            assertFalse(signals.lovable)
            assertTrue(signals.vercel)
            assertFalse(signals.supabase)
            assertEquals(3, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test fun insufficientGitHubPermissionIsExplainedWithoutLeakingToken() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(403).setBody("PRIVATE DATA"))
            var failed = false
            try {
                AltivGitHubProjectInspector(apiBase = server.url("/").toString())
                    .inspect("supersecret", "owner/private")
            } catch (e: AltivGitHubAccessException) {
                failed = true
                assertTrue(e.message.orEmpty().contains("Contents: Read"))
                assertFalse(e.message.orEmpty().contains("supersecret"))
                assertFalse(e.message.orEmpty().contains("PRIVATE DATA"))
            }
            assertTrue(failed)
        } finally {
            server.shutdown()
        }
    }

    @Test fun rejectsUserControlledPathsBeforeAnyRequest() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            var failed = false
            try {
                AltivGitHubProjectInspector(apiBase = server.url("/").toString())
                    .inspect("secret", "../unauthorized")
            } catch (_: AltivGitHubAccessException) {
                failed = true
            }
            assertTrue(failed)
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
