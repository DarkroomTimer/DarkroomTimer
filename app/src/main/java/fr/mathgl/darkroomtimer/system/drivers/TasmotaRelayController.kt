package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Credentials
import java.util.concurrent.TimeUnit

class TasmotaRelayController(
    private val host: String,
    private val port: Int = 80,
    private val channel: Int = 1,
    private val username: String? = null,
    private val password: String? = null,
    val timingMode: TimingMode = TimingMode.TIMED_POWER
) : RelayController {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var isConnected = false

    override val canPause: Boolean = timingMode == TimingMode.EXPLICIT_ON_OFF
    override val state = MutableStateFlow(RelayState.UNKNOWN)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private fun createRequest(cmnd: String): Request {
        val url = "http://$host:$port/cm?cmnd=$cmnd"
        val requestBuilder = Request.Builder().url(url)

        if (username != null && password != null) {
            requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
        }

        return requestBuilder.build()
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        connectionState.value = ConnectionState.Connecting
        try {
            val request = createRequest("Status 0")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    isConnected = true
                    connectionState.value = ConnectionState.Connected
                    Result.success(Unit)
                } else {
                    connectionState.value = ConnectionState.Error("Server returned ${response.code}")
                    Result.failure(Exception("Server returned ${response.code}"))
                }
            }
        } catch (e: Exception) {
            connectionState.value = ConnectionState.Error(e.message ?: "Unknown connection error")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        // Do not shut down the dispatcher executor: the controller must stay
        // usable for a later connect() on the same instance.
        okHttpClient.dispatcher.cancelAll()
        isConnected = false
        connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun set(on: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(Exception("Not connected"))
        val cmd = if (on) "Power$channel ON" else "Power$channel OFF"

        try {
            val request = createRequest(cmd)
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    state.value = if (on) RelayState.ON else RelayState.OFF
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Server returned ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startTimed(durationMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (timingMode == TimingMode.TIMED_POWER) {
            val cmd = "TimedPower$channel $durationMs"
            try {
                if (!isConnected) return@withContext Result.failure(Exception("Not connected"))
                val request = createRequest(cmd)
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        state.value = RelayState.ON
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Server returned ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            set(true)
        }
    }
}
