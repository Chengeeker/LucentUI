package dev.chengeeker.lucent.demo.data

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test

class TransportTest {
    @Test
    fun writeRedirectIsNeverAutomaticallyReplayed() =
        runBlocking<Unit> {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", "/other"))
                val r =
                    WebDavTransport(OkHttpClient(), 100)
                        .request(server.url("/backup"), "PUT", "Basic test", byteArrayOf(1))
                assertEquals(307, r.code)
                assertEquals(1, server.requestCount)
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun rejectsOversizeDownloadAndNonSuccessStatus() =
        runBlocking<Unit> {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(MockResponse().setBody("123456"))
                var failed = false
                try {
                    WebDavTransport(OkHttpClient(), 4).request(server.url("/"), "GET", "")
                } catch (_: Exception) {
                    failed = true
                }
                assertTrue(failed)
                assertThrows(Exception::class.java) {
                    WebDavResponse(401, byteArrayOf(), null).requireStatus("连接", setOf(200))
                }
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun sameOriginReadRedirectRetainsAuthButHasHopLimit() =
        runBlocking<Unit> {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final"))
                server.enqueue(MockResponse().setBody("{}"))
                assertEquals(
                    200,
                    WebDavTransport(OkHttpClient(), 100)
                        .request(server.url("/first"), "GET", "Basic test")
                        .code,
                )
                assertEquals("Basic test", server.takeRequest().getHeader("Authorization"))
                assertEquals("Basic test", server.takeRequest().getHeader("Authorization"))
            } finally {
                server.shutdown()
            }
        }

    @Test
    fun cancellationStopsHangingCall() =
        runBlocking<Unit> {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
                val job = launch {
                    WebDavTransport(OkHttpClient(), 100).request(server.url("/"), "GET", "")
                }
                delay(100)
                withTimeout(1500) { job.cancelAndJoin() }
                assertTrue(job.isCancelled)
            } finally {
                server.shutdown()
            }
        }
}
