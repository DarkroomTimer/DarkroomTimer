package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Base64

class RelayDriverTest {
    private lateinit var server: MockWebServer
    private lateinit var tasmotaController: TasmotaRelayController

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        tasmotaController = TasmotaRelayController(
            host = server.hostName,
            port = server.port
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `Tasmota connect should send Status 0 command`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.connect()
        val request: RecordedRequest = server.takeRequest()
        assertEquals("/cm?cmnd=Status%200", request.path)
    }

    @Test
    fun `Tasmota authentication should add Basic Auth header`() = runTest {
        val user = "admin"
        val pass = "password"
        val authController = TasmotaRelayController(
            host = server.hostName,
            port = server.port,
            username = user,
            password = pass
        )

        server.enqueue(MockResponse().setResponseCode(200))
        authController.connect()

        val request: RecordedRequest = server.takeRequest()
        val expectedAuth = "Basic " + Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
        assertEquals(expectedAuth, request.getHeader("Authorization"))
    }

    @Test
    fun `Tasmota set should send PowerX ON or OFF`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.connect()
        server.takeRequest()

        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.set(true)
        val reqOn = server.takeRequest()
        assertEquals("/cm?cmnd=Power1%20ON", reqOn.path)

        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.set(false)
        val reqOff = server.takeRequest()
        assertEquals("/cm?cmnd=Power1%20OFF", reqOff.path)
    }

    @Test
    fun `Tasmota startTimed should send PowerX seconds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.connect()
        server.takeRequest()

        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.startTimed(2000L)

        val request: RecordedRequest = server.takeRequest()
        assertEquals("/cm?cmnd=Power1%202", request.path)
    }

    @Test
    fun `Tasmota connect failure should set ConnectionState Error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = tasmotaController.connect()
        assertEquals(false, result.isSuccess)
        assertEquals(ConnectionState.Error("Server returned 500"), tasmotaController.connectionState.value)
    }

    @Test
    fun `ESPhome connect should send HEAD request`() = runTest {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()

        val request: RecordedRequest = server.takeRequest()
        assertEquals("HEAD", request.method)
        assertEquals("/", request.path)
    }

    @Test
    fun `ESPhome set should send POST with JSON body`() = runTest {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()
        server.takeRequest()

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.set(true)

        val request: RecordedRequest = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/switch.set", request.path)
        assertEquals("{\"entity_id\":\"switch.light\",\"state\":true}", request.body.readUtf8())
    }

    @Test
    fun `ESPhome startTimed should fallback to simple set`() = runTest {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()
        server.takeRequest()

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.startTimed(2000L)

        val request: RecordedRequest = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("{\"entity_id\":\"switch.light\",\"state\":true}", request.body.readUtf8())
    }
}
