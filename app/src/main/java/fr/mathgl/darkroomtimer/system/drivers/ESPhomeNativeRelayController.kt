package fr.mathgl.darkroomtimer.system.drivers

import android.util.Log
import fr.mathgl.darkroomtimer.system.*
import fr.mathgl.darkroomtimer.system.drivers.noise.NoiseNNpsk0
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

private const val TAG = "DT/ESPhomeNative"
private const val SO_TIMEOUT_MS = 10_000

// ESPHome Native API relay controller using the Noise_NNpsk0_25519_ChaChaPoly_SHA256 protocol.
// canPause=true: the app controls timing externally (EXPLICIT_ON_OFF mode).
class ESPhomeNativeRelayController(
    private val host: String,
    private val port: Int = 6053,
    private val entityId: String,       // objectId from ListEntitiesSwitchResponse (e.g. "relay_1")
    private val encryptionKey: ByteArray // 32-byte raw PSK (decoded from base64)
) : RelayController {

    override val canPause = true
    override val state = MutableStateFlow(RelayState.UNKNOWN)
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var noise: NoiseNNpsk0? = null
    private var entityKey: Int? = null
    private val mutex = Mutex()

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            connectionState.value = ConnectionState.Connecting
            Log.d(TAG, "connect: $host:$port entity=$entityId pskLen=${encryptionKey.size} pskHead=${encryptionKey.take(4).joinToString("") { "%02x".format(it) }}")
            try {
                doConnect()
                Log.d(TAG, "connect: success, entityKey=${entityKey}")
                connectionState.value = ConnectionState.Connected
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "connect: failed", e)
                closeSocket()
                connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                Result.failure(e)
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            Log.d(TAG, "disconnect")
            try {
                val out = outputStream
                val n = noise
                if (out != null && n != null && socket?.isConnected == true) {
                    sendEncrypted(EspHomeProto.MSG_DISCONNECT_REQUEST, EspHomeProto.encodeDisconnectRequest(), out, n)
                }
            } catch (_: Exception) { }
            closeSocket()
            connectionState.value = ConnectionState.Disconnected
        }
    }

    override suspend fun set(on: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val key = entityKey
            val out = outputStream
            val n = noise
            if (key == null || out == null || n == null) {
                return@withContext Result.failure(Exception("Not connected"))
            }
            Log.d(TAG, "set: $entityId → $on (key=$key)")
            try {
                sendEncrypted(EspHomeProto.MSG_SWITCH_COMMAND_REQUEST, EspHomeProto.encodeSwitchCommand(key, on), out, n)
                state.value = if (on) RelayState.ON else RelayState.OFF
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "set: failed", e)
                closeSocket()
                connectionState.value = ConnectionState.Error(e.message ?: "Send failed")
                Result.failure(e)
            }
        }
    }

    // canPause=true: caller controls duration. Just turn on; caller will call set(false) later.
    override suspend fun startTimed(durationMs: Long): Result<Unit> = set(true)

    // --- Connection flow ---

    private fun doConnect() {
        val sock = Socket(host, port).also {
            it.soTimeout = SO_TIMEOUT_MS
            socket = it
        }
        val out = sock.getOutputStream().also { outputStream = it }
        val inp = sock.getInputStream().also { inputStream = it }

        // Noise handshake
        val n = NoiseNNpsk0(encryptionKey).also { noise = it }
        performNoiseHandshake(inp, out, n)

        // Application-level hello
        sendEncrypted(EspHomeProto.MSG_HELLO_REQUEST, EspHomeProto.encodeHelloRequest(), out, n)
        val (helloType, helloPayload) = recvEncrypted(inp, n)
        require(helloType == EspHomeProto.MSG_HELLO_RESPONSE) {
            "Expected HelloResponse (2), got $helloType"
        }
        val helloResp = EspHomeProto.decodeHelloResponse(helloPayload)
        Log.d(TAG, "hello ok: API ${helloResp.apiVersionMajor}.${helloResp.apiVersionMinor} server=${helloResp.serverInfo}")

        // Discover entity key
        sendEncrypted(EspHomeProto.MSG_LIST_ENTITIES_REQUEST, EspHomeProto.encodeListEntitiesRequest(), out, n)
        var foundKey: Int? = null
        loop@ while (true) {
            val (type, payload) = recvEncrypted(inp, n)
            when (type) {
                EspHomeProto.MSG_LIST_ENTITIES_SWITCH_RESPONSE -> {
                    Log.d(TAG, "switch entity raw: ${payload.joinToString("") { "%02x".format(it) }}")
                    val entity = EspHomeProto.decodeSwitchEntity(payload)
                    Log.d(TAG, "switch entity: objectId=${entity.objectId} key=${entity.key} name=${entity.name}")
                    if (entity.objectId == entityId || entity.name == entityId) foundKey = entity.key
                }
                EspHomeProto.MSG_LIST_ENTITIES_DONE_RESPONSE -> break@loop
                EspHomeProto.MSG_DISCONNECT_REQUEST -> throw Exception("Device requested disconnect during entity listing")
            }
        }
        entityKey = foundKey ?: throw Exception("Entity '$entityId' not found in device listing")
    }

    // Sends a combined NOISE_HELLO + client handshake frame, then reads server hello and
    // server handshake frames to complete the NNpsk0 two-message exchange.
    private fun performNoiseHandshake(inp: InputStream, out: OutputStream, n: NoiseNNpsk0) {
        val clientPayload = n.buildClientHandshakePayload()  // \x00 + pub[32] + mac[16]
        val frameLen = clientPayload.size
        val clientFrame = byteArrayOf(
            0x01,
            ((frameLen shr 8) and 0xFF).toByte(),
            (frameLen and 0xFF).toByte()
        ) + clientPayload
        Log.d(TAG, "noise: sending client handshake frame ${clientPayload.size} bytes")

        // Combined write: NOISE_HELLO (\x01\x00\x00) + client handshake frame
        out.write(byteArrayOf(0x01, 0x00, 0x00) + clientFrame)
        out.flush()

        // Read server hello frame (protocol negotiation — we accept 0x01)
        val serverHelloContent = EspHomeProto.readNoiseOuterFrame(inp)
        Log.d(TAG, "server hello: ${serverHelloContent.size} bytes, proto=0x${serverHelloContent.getOrNull(0)?.toUByte()?.toString(16)}")
        require(serverHelloContent.isNotEmpty()) { "ServerHello frame is empty" }
        require(serverHelloContent[0] == 0x01.toByte()) {
            "Server chose unknown protocol: 0x${serverHelloContent[0].toUByte().toString(16)}"
        }

        // Read server handshake frame: \x00 + serverPub[32] + serverMac[16]
        val serverHandshakeFrame = EspHomeProto.readNoiseOuterFrame(inp)
        Log.d(TAG, "noise: server handshake frame ${serverHandshakeFrame.size} bytes, preamble=0x${serverHandshakeFrame.getOrNull(0)?.toUByte()?.toString(16)}")
        require(serverHandshakeFrame.isNotEmpty()) { "Server handshake frame is empty" }
        require(serverHandshakeFrame[0] == 0x00.toByte()) {
            // Non-zero preamble means error — server sent an error string
            val errBytes = serverHandshakeFrame.copyOfRange(1, serverHandshakeFrame.size)
            val explanation = errBytes.toString(Charsets.UTF_8)
            Log.e(TAG, "noise: server error hex=${errBytes.joinToString("") { "%02x".format(it) }} msg=$explanation")
            if (explanation == "Handshake MAC failure") {
                throw Exception("Invalid encryption key (PSK mismatch)")
            }
            throw Exception("Handshake error: $explanation")
        }
        // serverHandshakeFrame[1..49] = serverPub[32] + serverMac[16]
        n.processServerHandshakePayload(serverHandshakeFrame.copyOfRange(1, serverHandshakeFrame.size))
        Log.d(TAG, "noise handshake complete")
    }

    // --- Encrypted message send/recv ---

    private fun sendEncrypted(msgType: Int, payload: ByteArray, out: OutputStream, n: NoiseNNpsk0) {
        val plaintext = EspHomeProto.buildNoiseFramePlaintext(msgType, payload)
        val ciphertext = n.encryptMessage(plaintext)
        val frame = EspHomeProto.buildNoiseOuterFrame(ciphertext)
        out.write(frame)
        out.flush()
    }

    private fun recvEncrypted(inp: InputStream, n: NoiseNNpsk0): Pair<Int, ByteArray> {
        val ciphertext = EspHomeProto.readNoiseOuterFrame(inp)
        val plaintext = n.decryptMessage(ciphertext)
        return EspHomeProto.parseNoisePlaintext(plaintext)
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: Exception) { }
        socket = null
        inputStream = null
        outputStream = null
        noise = null
        entityKey = null
    }
}
