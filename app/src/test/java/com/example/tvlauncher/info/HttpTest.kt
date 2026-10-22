package com.example.tvlauncher.info

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket

class HttpTest {

    private var server: HttpServer? = null

    private fun serve(status: Int, body: String): String {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/") { exchange ->
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        s.start()
        server = s
        return "http://127.0.0.1:${s.address.port}/"
    }

    @After
    fun stop() {
        server?.stop(0)
    }

    @Test
    fun okReturnsBody() {
        val url = serve(200, """{"ok":true}""")

        assertEquals(HttpResult.Ok("""{"ok":true}"""), Http.request(url))
        assertEquals("""{"ok":true}""", Http.get(url))
    }

    @Test
    fun errorStatusMeansServerAnswered() {
        val url = serve(429, "Too Many Requests")

        assertEquals(HttpResult.Status(429), Http.request(url))
        assertNull(Http.get(url))
    }

    @Test
    fun closedPortIsUnreachable() {
        val port = ServerSocket(0).use { it.localPort }

        assertEquals(HttpResult.Unreachable, Http.request("http://127.0.0.1:$port/"))
    }
}
