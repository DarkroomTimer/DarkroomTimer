package fr.mathgl.darkroomtimer.system.drivers

import android.util.Log
import com.google.gson.Gson
import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "DT/ESPhome"

class ESPhomeHttpRelayController(
    private val host: String,
    private val port: Int = 80,
    private val entityId: String
) : RelayController {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var isConnected = false
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val canPause: Boolean = false
    override val state = MutableStateFlow(RelayState.UNKNOWN)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "connect: $host:$port entity=$entityId")
        try {
            val request = Request.Builder()
                .url("http://$host:$port/")
                .head()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    isConnected = true
                    connectionState.value = ConnectionState.Connected
                    Log.d(TAG, "connect: success (HTTP ${response.code})")
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
        // Do not shut down the dispatcher executor: the controller must stay
        // usable for a later connect() on the same instance.
        okHttpClient.dispatcher.cancelAll()
        isConnected = false
        connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun set(on: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext Result.failure(Exception("Not connected"))
        Log.d(TAG, "set: entity=$entityId on=$on")

        val bodyMap = mapOf(
            "entity_id" to entityId,
            "state" to on
        )
        val jsonBody = gson.toJson(bodyMap)
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("http://$host:$port/api/switch.set")
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    state.value = if (on) RelayState.ON else RelayState.OFF
                    Log.d(TAG, "set: OK → ${state.value}")
                    Result.success(Unit)
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

    override suspend fun startTimed(durationMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        // ESPhome HTTP API usually doesn't support timed pulses in a single request
        // so we just do a simple set(true). The higher level timer will handle the timeout.
        Log.d(TAG, "startTimed: ${durationMs}ms → fallback to set(true), timed pulse not supported")
        set(true)
    }
}
