package com.subconverter.domain

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GistUploaderTest {
    private lateinit var server: MockWebServer
    private lateinit var uploader: GistUploader

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        uploader = GistUploader(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun createsGistWhenGistIdBlank() {
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    """
                    {"id":"abc123","files":{"My.yml":{"raw_url":"https://raw.example/My.yml"}}}
                    """.trimIndent(),
                ),
        )

        val result = runBlocking {
            uploader.upload(
                token = "tok",
                gistId = "",
                filename = "My.yml",
                content = "proxies: []",
            )
        }

        assertTrue(result.success)
        assertEquals("abc123", result.gistId)
        assertEquals("https://raw.example/My.yml", result.rawUrl)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/gists", recorded.path)
        assertEquals("Bearer tok", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"public\":false"))
        assertTrue(body.contains("\"My.yml\""))
        assertTrue(body.contains("\"proxies: []\""))
    }

    @Test
    fun updatesGistWhenGistIdPresent() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {"id":"abc123","files":{"My.yml":{"raw_url":"https://raw.example/My.yml"}}}
                    """.trimIndent(),
                ),
        )

        val result = runBlocking {
            uploader.upload(
                token = "tok",
                gistId = "abc123",
                filename = "My.yml",
                content = "proxies: []",
            )
        }

        assertTrue(result.success)
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/gists/abc123", recorded.path)
    }

    @Test
    fun fallsBackToCreateOnPatch404() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody("""{"id":"newid","files":{"My.yml":{"raw_url":"u"}}}"""),
        )

        val result = runBlocking { uploader.upload("tok", "stale", "My.yml", "proxies: []") }

        assertTrue(result.success)
        assertEquals("newid", result.gistId)
        assertEquals("PATCH", server.takeRequest().method)
        assertEquals("POST", server.takeRequest().method)
    }

    @Test
    fun mapsUnauthorizedToMessage() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val result = runBlocking { uploader.upload("bad", "", "My.yml", "proxies: []") }

        assertFalse(result.success)
        assertEquals("Gist Token 无效", result.message)
    }

    @Test
    fun mapsRateLimitToMessage() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))

        val result = runBlocking { uploader.upload("bad", "", "My.yml", "proxies: []") }

        assertFalse(result.success)
        assertEquals("GitHub 限流，稍后再试", result.message)
    }
}
