package dev.thingscompanion.cloud

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ThingsCloudClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ThingsCloudClient

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "install-1")
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    @Test
    fun `authenticate sends the password header and returns the history key`() {
        server.enqueue(MockResponse().setBody("""{"history-key": "hk-123"}"""))

        val key = client.authenticate("me+x@example.com", "p@ss word")

        assertEquals("hk-123", key)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/version/1/account/me%2Bx%40example.com", request.path)
        assertEquals("Password p%40ss%20word", request.getHeader("Authorization"))
        assertEquals("com.culturedcode.ThingsMac", request.getHeader("App-Id"))
        assertEquals("301", request.getHeader("Schema"))
        assertEquals("install-1", request.getHeader("App-Instance-Id"))
    }

    @Test
    fun `pull pages from the start index until the history is caught up`() {
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"a": {"t": 0, "e": "Task6", "p": {"tt": "A"}}}],
                   "current-item-index": 2, "end-total-content-size": 10, "latest-total-content-size": 20}"""
            )
        )
        server.enqueue(
            MockResponse().setBody(
                """{"items": [{"a": {"t": 1, "e": "Task6", "p": {"tt": "A2"}}}],
                   "current-item-index": 2, "end-total-content-size": 20, "latest-total-content-size": 20}"""
            )
        )
        val state = HistoryState()

        val next = client.pull("hk", state, startIndex = 5)

        assertEquals(7, next)
        assertEquals("/version/1/history/hk/items?start-index=5", server.takeRequest().path)
        assertEquals("/version/1/history/hk/items?start-index=6", server.takeRequest().path)
        assertEquals("A2", ThingsLibrary.from(state).task("a")!!.title)
    }

    @Test
    fun `http errors surface as ThingsCloudException with the status`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("nope"))

        val error = assertFailsWith<ThingsCloudException> { client.authenticate("a@b.c", "bad") }

        assertEquals(401, error.status)
    }

    @Test
    fun `history errors never expose the history key`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("down"))

        val error = assertFailsWith<ThingsCloudException> { client.pull("secret-key-42", HistoryState()) }

        assertEquals(503, error.status)
        assertFalse(error.message!!.contains("secret-key-42"), error.message)
        assertFalse(error.stackTraceToString().contains("secret-key-42"))
    }
}
