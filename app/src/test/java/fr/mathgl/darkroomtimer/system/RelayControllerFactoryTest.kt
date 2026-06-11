package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.DemoRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.NullRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RelayControllerFactoryTest {

    @Test
    fun `Null config creates NullRelayController`() {
        val controller = RelayControllerFactory.create(RelayControllerConfig.Null, Dispatchers.Unconfined)
        assertTrue(controller is NullRelayController)
    }

    @Test
    fun `Demo config creates DemoRelayController`() {
        val controller = RelayControllerFactory.create(RelayControllerConfig.Demo, Dispatchers.Unconfined)
        assertTrue(controller is DemoRelayController)
    }

    @Test
    fun `Tasmota config creates TasmotaRelayController with timing mode`() {
        val timed = RelayControllerFactory.create(
            RelayControllerConfig.Tasmota(host = "10.0.0.2", timingMode = TimingMode.TIMED_POWER),
            Dispatchers.Unconfined
        )
        assertTrue(timed is TasmotaRelayController)
        assertFalse(timed.canPause)

        val explicit = RelayControllerFactory.create(
            RelayControllerConfig.Tasmota(host = "10.0.0.2", timingMode = TimingMode.EXPLICIT_ON_OFF),
            Dispatchers.Unconfined
        )
        assertTrue(explicit.canPause)
    }

    @Test
    fun `ESPhomeHttp config creates ESPhomeHttpRelayController`() {
        val controller = RelayControllerFactory.create(
            RelayControllerConfig.ESPhomeHttp(host = "10.0.0.3", entityId = "switch.enlarger"),
            Dispatchers.Unconfined
        )
        assertTrue(controller is ESPhomeHttpRelayController)
    }

    @Test
    fun `Tasmota with blank credentials should not send Authorization header`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val controller = RelayControllerFactory.create(
                RelayControllerConfig.Tasmota(
                    host = server.hostName,
                    port = server.port,
                    username = "",
                    password = ""
                ),
                Dispatchers.Unconfined
            )

            server.enqueue(MockResponse().setResponseCode(200))
            controller.connect()

            val request = server.takeRequest(10, TimeUnit.SECONDS)!!
            assertNull(request.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}
