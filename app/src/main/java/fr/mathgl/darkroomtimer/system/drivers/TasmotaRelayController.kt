package fr.mathgl.darkroomtimer.system.drivers

import android.util.Log
import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Credentials
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.concurrent.TimeUnit

private const val TAG = "DT/Tasmota"

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

    private val gson = Gson()
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
        Log.d(TAG, "connect: $host:$port channel=$channel timingMode=$timingMode")
        try {
            val request = createRequest("Status 0")
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    isConnected = true
                    connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "connect: success (HTTP ${response.code})")
                    if (timingMode == TimingMode.TIMED_POWER) {
                        clearPulseTime()
                    }
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "connect: HTTP ${response.code}")
                    connectionState.value = ConnectionState.Error("Server returned ${response.code}")
                    Result.failure(Exception("Server returned ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect: exception", e)
            connectionState.value = ConnectionState.Error(e.message ?: "Unknown connection error")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        isConnected = false
        okHttpClient.dispatcher.cancelAll()
        connectionState.value = ConnectionState.Disconnected
    }

    // PulseTime caps TimedPower duration. Disable it so the configured exposure time is respected.
    private suspend fun clearPulseTime() = withContext(Dispatchers.IO) {
        try {
            okHttpClient.newCall(createRequest("PulseTime$channel 0")).execute().use { response ->
                Log.d(TAG, "clearPulseTime: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "clearPulseTime: failed (non-fatal) — ${e.message}")
        }
    }

    override suspend fun set(on: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(Exception("Not connected"))
        val cmd = if (on) "Power$channel ON" else "Power$channel OFF"
        Log.d(TAG, "set: $cmd")

        try {
            val request = createRequest(cmd)
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null && verifyResponse(body, "Power$channel")) {
                        state.value = if (on) RelayState.ON else RelayState.OFF
                        Log.d(TAG, "set: OK → ${state.value}")
                        Result.success(Unit)
                    } else {
                        Log.e(TAG, "set: invalid response body=$body")
                        Result.failure(Exception("Tasmota returned invalid response: $body"))
                    }
                } else {
                    Log.e(TAG, "set: HTTP ${response.code}")
                    Result.failure(Exception("Server returned ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "set: exception", e)
            Result.failure(e)
        }
    }

    private fun verifyResponse(json: String, key: String): Boolean {
        return try {
            val obj = gson.fromJson(json, JsonObject::class.java)
            obj.keySet().any { it.equals(key, ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun startTimed(durationMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (timingMode == TimingMode.TIMED_POWER) {
            if (!isConnected) return@withContext Result.failure(Exception("Not connected"))
            Log.d(TAG, "startTimed: TimedPower$channel ${durationMs}ms")
            try {
                okHttpClient.newCall(createRequest("TimedPower$channel $durationMs")).execute().use { response ->
                    val body = response.body?.string()
                    // Tasmota confirms TimedPower with {"POWER<x>":"ON"}
                    if (response.isSuccessful && body != null && verifyResponse(body, "Power$channel")) {
                        state.value = RelayState.ON
                        Log.d(TAG, "startTimed: OK")
                        Result.success(Unit)
                    } else {
                        Log.e(TAG, "startTimed: invalid response HTTP=${response.code} body=$body")
                        Result.failure(Exception("Tasmota returned invalid response: $body"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "startTimed: exception", e)
                Result.failure(e)
            }
        } else {
            set(true)
        }
    }
}
