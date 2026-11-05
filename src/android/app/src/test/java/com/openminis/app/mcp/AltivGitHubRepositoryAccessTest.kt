package com.openminis.app.mcp

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivGitHubRepositoryAccessTest {
    @Test fun tokenShowsOnlyRepositoriesReturnedByAuthenticatedApi() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"login":"altiv-user"}"""))
            server.enqueue(MockResponse().setBody(
                """[{"full_name":"altiv-user/private","private":true},""" +
                    """{"full_name":"altiv-user/public","private":false}]"""
            ))
            val scan = AltivGitHubRepositoryAccess(apiBase = server.url("/").toString())
                .inspect("test-token")
            assertEquals("altiv-user", scan.account)
            assertEquals(2, scan.repositories.size)
            assertTrue(scan.complete)
            assertEquals("altiv-user/private", scan.repositories[0].fullName)
            assertTrue(scan.repositories[0].isPrivate)
            assertFalse(scan.repositories[1].isPrivate)

            val userRequest = server.takeRequest()
            val reposRequest = server.takeRequest()
            assertEquals("/user", userRequest.path)
            assertTrue(reposRequest.path!!.startsWith("/user/repos?"))
            assertEquals("Bearer test-token", userRequest.getHeader("Authorization"))
            assertEquals("Bearer test-token", reposRequest.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun repositoryListingReadsNextPageWhenFirstHas100() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val page1 = (1..100).joinToString(",") {
                """{"full_name":"owner/repo""" + it + """","private":false}"""
            }
            server.enqueue(MockResponse().setBody("""{"login":"owner"}"""))
            server.enqueue(MockResponse().setBody("[" + page1 + "]"))
            server.enqueue(MockResponse().setBody("""[{"full_name":"owner/repo101","private":true}]"""))
            val scan = AltivGitHubRepositoryAccess(apiBase = server.url("/").toString())
                .inspect("test-token")
            assertEquals(101, scan.repositories.size)
            assertTrue(scan.complete)
            assertEquals(3, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test fun deniedRepositoryEnumerationIsNotReportedAsConnected() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("""{"login":"owner"}"""))
            server.enqueue(MockResponse().setResponseCode(403).setBody("forbidden"))
            var failed = false
            try {
                AltivGitHubRepositoryAccess(apiBase = server.url("/").toString())
                    .inspect("private-token")
            } catch (e: AltivGitHubAccessException) {
                failed = true
                assertTrue(e.message.orEmpty().contains("Metadata"))
                assertFalse(e.message.orEmpty().contains("private-token"))
            }
            assertTrue(failed)
        } finally {
            server.shutdown()
        }
    }
}
