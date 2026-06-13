package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Base64
import java.util.concurrent.TimeUnit

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
        runBlocking { tasmotaController.disconnect() }
        server.shutdown()
    }

    // Enqueue Status 0 + PulseTime responses, connect, consume both requests.
    private fun connectTasmota(controller: TasmotaRelayController = tasmotaController) = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        controller.connect()
        server.takeRequest(10, TimeUnit.SECONDS)!!
        server.takeRequest(10, TimeUnit.SECONDS)!!
    }

    @Test
    fun `Tasmota connect should send Status 0 command`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.connect()
        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("/cm?cmnd=Status%200", request.path)
    }

    @Test
    fun `Tasmota connect should clear PulseTime in TIMED_POWER mode`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        tasmotaController.connect()
        server.takeRequest(10, TimeUnit.SECONDS)!!  // Status 0
        val pulseTimeRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("/cm?cmnd=PulseTime1%200", pulseTimeRequest.path)
    }

    @Test
    fun `Tasmota authentication should add Basic Auth header`() = runBlocking {
        val user = "admin"
        val pass = "password"
        val authController = TasmotaRelayController(
            host = server.hostName,
            port = server.port,
            username = user,
            password = pass
        )

        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        authController.connect()

        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        val expectedAuth = "Basic " + Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
        assertEquals(expectedAuth, request.getHeader("Authorization"))
    }

    @Test
    fun `Tasmota set should fail if JSON response is invalid`() = runBlocking {
        connectTasmota()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"wrong_key\": \"value\"}"))
        val result = tasmotaController.set(true)
        assertEquals(false, result.isSuccess)
        assertEquals("Tasmota returned invalid response: {\"wrong_key\": \"value\"}", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Tasmota set should succeed if JSON response is valid`() = runBlocking {
        connectTasmota()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"Power1\": \"ON\"}"))
        val result = tasmotaController.set(true)
        assertEquals(true, result.isSuccess)
    }

    @Test
    fun `Tasmota startTimed should send TimedPowerX milliseconds`() = runBlocking {
        connectTasmota()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"Power1\": \"ON\"}"))
        tasmotaController.startTimed(2000L)

        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("/cm?cmnd=TimedPower1%202000", request.path)
    }

    @Test
    fun `Tasmota startTimed should succeed if JSON response is valid`() = runBlocking {
        connectTasmota()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"Power1\": \"ON\"}"))
        val result = tasmotaController.startTimed(2000L)
        assertEquals(true, result.isSuccess)
    }

    @Test
    fun `Tasmota startTimed should fail if JSON response is invalid`() = runBlocking {
        connectTasmota()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"wrong_key\": \"value\"}"))
        val result = tasmotaController.startTimed(2000L)
        assertEquals(false, result.isSuccess)
        assertEquals("Tasmota returned invalid response: {\"wrong_key\": \"value\"}", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Tasmota should reconnect after disconnect`() = runBlocking {
        connectTasmota()
        tasmotaController.disconnect()
        assertEquals(ConnectionState.Disconnected, tasmotaController.connectionState.value)

        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        val result = tasmotaController.connect()
        assertEquals(true, result.isSuccess)
        assertEquals(ConnectionState.Connected, tasmotaController.connectionState.value)
    }

    @Test
    fun `ESPhome should reconnect after disconnect`() = runBlocking {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()
        server.takeRequest(10, TimeUnit.SECONDS)!!

        esphomeController.disconnect()

        server.enqueue(MockResponse().setResponseCode(200))
        val result = esphomeController.connect()
        assertEquals(true, result.isSuccess)
        assertEquals(ConnectionState.Connected, esphomeController.connectionState.value)
    }

    @Test
    fun `Tasmota connect failure should set ConnectionState Error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = tasmotaController.connect()
        assertEquals(false, result.isSuccess)
        assertEquals(ConnectionState.Error("Server returned 500"), tasmotaController.connectionState.value)
    }

    @Test
    fun `ESPhome connect should send HEAD request`() = runBlocking {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()

        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("HEAD", request.method)
        assertEquals("/", request.path)
    }

    @Test
    fun `ESPhome set should send POST with JSON body`() = runBlocking {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()
        server.takeRequest(10, TimeUnit.SECONDS)!!

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.set(true)

        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/api/switch.set", request.path)
        assertEquals("{\"entity_id\":\"switch.light\",\"state\":true}", request.body.readUtf8())
    }

    @Test
    fun `ESPhome startTimed should fallback to simple set`() = runBlocking {
        val esphomeController = ESPhomeHttpRelayController(
            host = server.hostName,
            port = server.port,
            entityId = "switch.light"
        )

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.connect()
        server.takeRequest(10, TimeUnit.SECONDS)!!

        server.enqueue(MockResponse().setResponseCode(200))
        esphomeController.startTimed(2000L)

        val request: RecordedRequest = server.takeRequest(10, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("{\"entity_id\":\"switch.light\",\"state\":true}", request.body.readUtf8())
    }
}
