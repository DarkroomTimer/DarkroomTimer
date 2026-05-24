package fr.mathgl.darkroomtimer.system.drivers

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

class ESPhomeHttpRelayController(
    private val host: String,
    private val port: Int = 80,
    private val entityId: String
) : RelayController {

    private var client: OkHttpClient? = null
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override val canPause: Boolean = false
    override val state = MutableStateFlow(RelayState.UNKNOWN)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        connectionState.value = ConnectionState.Connecting
        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            // Simple reachability check: try to get the root page or a known endpoint
            val request = Request.Builder()
                .url("http://$host:$port/")
                .head()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    client = okHttpClient
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
        client?.dispatcher?.executorService?.shutdown()
        client = null
        connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun set(on: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val currentClient = client ?: return@withContext Result.failure(Exception("Not connected"))

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
            currentClient.newCall(request).execute().use { response ->
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
        // ESPhome HTTP API usually doesn't support timed pulses in a single request
        // so we just do a simple set(true). The higher level timer will handle the timeout.
        set(true)
    }
}
