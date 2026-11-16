package dev.thingscompanion.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThingsSyncTest {

    private class MemoryCache : HistoryCache {
        var saved: Pair<HistoryState, Long>? = null
        var snapshot: WidgetSnapshot? = null
        override fun load() = saved?.let { HistoryState.fromJson(it.first.toJson()) to it.second }
        override fun save(state: HistoryState, nextStartIndex: Long, snapshot: WidgetSnapshot) {
            saved = HistoryState.fromJson(state.toJson()) to nextStartIndex
            this.snapshot = snapshot
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var cache: MemoryCache
    private lateinit var sync: ThingsSync

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        cache = MemoryCache()
        val client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "i")
        sync = ThingsSync(client, cache, now = { 1_794_700_000.0 }, today = { LocalDate.of(2026, 11, 15) })
    }

    @AfterTest
    fun tearDown() = server.shutdown()

    private fun page(items: String = "", head: Long) = MockResponse().setBody(
        """{"items": [$items], "current-item-index": $head, "end-total-content-size": 1, "latest-total-content-size": 1}"""
    )

    private fun created(id: String, title: String, notes: String = "") =
        """{"$id": ${InboxTask.payload(title, notes, index = 0, now = 1.0)}}"""

    @Test
    fun `sync resumes from the cached index and saves state, index and snapshot`() {
        server.enqueue(page(created("a", "First"), head = 10))
        sync.sync("hk")
        server.enqueue(page(created("b", "Second"), head = 11))

        val result = sync.sync("hk")

        assertEquals("/version/1/history/hk/items?start-index=0", server.takeRequest().path)
        assertEquals("/version/1/history/hk/items?start-index=1", server.takeRequest().path)
        assertEquals(11, result.headIndex)
        assertNotNull(result.library.task("a"))
        assertEquals(2, cache.saved!!.second)
        assertEquals(setOf("a", "b"), cache.snapshot!!.items("inbox").map { it.id }.toSet())
    }

    @Test
    fun `add pulls first, commits against the server head, then confirms`() {
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 41}"""))
        server.enqueue(page(created("new", "Buy milk"), head = 41))

        val library = sync.addInboxTask("hk", id = "new", title = "Buy milk", notes = "")

        assertNotNull(library.task("new"))
        server.takeRequest()
        val commit = server.takeRequest()
        assertEquals("/version/1/history/hk/commit?ancestor-index=40&_cnt=1", commit.path)
        val body = Json.parseToJsonElement(commit.body.readUtf8()).jsonObject
        assertEquals(setOf("new"), body.keys)
    }

    @Test
    fun `a landed attempt with the same text is success without a second commit`() {
        server.enqueue(page(created("new", "Buy milk"), head = 41))

        sync.addInboxTask("hk", id = "new", title = "Buy milk", notes = "")

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a landed attempt with other text fails visibly and sends nothing`() {
        server.enqueue(page(created("new", "Buy milk"), head = 41))

        val error = assertFailsWith<EarlierAttemptLandedException> {
            sync.addInboxTask("hk", id = "new", title = "Buy oat milk", notes = "")
        }

        assertEquals("Buy milk", error.savedTitle)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a commit that never shows up is reported as not confirmed`() {
        server.enqueue(page(head = 40))
        server.enqueue(MockResponse().setBody("""{"server-head-index": 41}"""))
        server.enqueue(page(head = 41))

        assertFailsWith<TaskNotConfirmedException> { sync.addInboxTask("hk", id = "new", title = "Buy milk", notes = "") }
    }

    @Test
    fun `landed tells whether an id is in the history after a fresh pull`() {
        server.enqueue(page(created("new", "Buy milk"), head = 41))
        assertTrue(sync.landed("hk", "new"))

        server.enqueue(page(head = 41))
        assertFalse(sync.landed("hk", "other"))
    }

    @Test
    fun `nothing is saved when the caller aborts before saving`() {
        server.enqueue(page(created("a", "First"), head = 10))

        assertFailsWith<IllegalStateException> { sync.sync("hk", beforeSave = { error("signed out") }) }

        assertNull(cache.saved)
    }

    @Test
    fun `the widget snapshot already shows changes not sent yet`() {
        val client = ThingsCloudClient(baseUrl = server.url("/version/1").toString(), appInstanceId = "i")
        val pending = listOf(PendingChange.AddTask("o", "queued", "Offline idea", "", PendingChange.When.of(Schedule.Inbox)))
        val withQueue = ThingsSync(client, cache, now = { 1_794_700_000.0 }, today = { LocalDate.of(2026, 11, 15) }, pending = { pending })
        server.enqueue(page(created("a", "First"), head = 10))

        val result = withQueue.sync("hk")

        assertNull(result.library.task("queued"))
        assertEquals(setOf("a", "queued"), cache.snapshot!!.items("inbox").map { it.id }.toSet())
    }
}
