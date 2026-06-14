package fr.mathgl.darkroomtimer.system.drivers

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Manual protobuf encoding/decoding for the ESPHome Native API messages we use.
//
// Post-handshake frame format (noise, per aioesphomeapi/packets.py):
//   Send: \x01 | uint16_BE(ciphertext_len) | ciphertext
//   The encrypted plaintext = uint16_BE(msgType) | uint16_BE(payloadLen) | payload
//
// Plaintext frame format (for dev/test without noise):
//   \x00 | varint(payloadLen) | varint(msgType) | payload
internal object EspHomeProto {

    // Message type IDs from api.proto option (id)
    const val MSG_HELLO_REQUEST = 1
    const val MSG_HELLO_RESPONSE = 2
    const val MSG_DISCONNECT_REQUEST = 5
    const val MSG_DISCONNECT_RESPONSE = 6
    const val MSG_PING_REQUEST = 7
    const val MSG_PING_RESPONSE = 8
    const val MSG_LIST_ENTITIES_REQUEST = 11
    const val MSG_LIST_ENTITIES_SWITCH_RESPONSE = 17
    const val MSG_LIST_ENTITIES_DONE_RESPONSE = 19
    const val MSG_SWITCH_COMMAND_REQUEST = 33

    // --- Frame builders ---

    // Builds the 4-byte header + payload that gets encrypted as one unit (noise mode).
    // Layout: uint16_BE(msgType) + uint16_BE(payloadLen) + payload
    fun buildNoiseFramePlaintext(msgType: Int, payload: ByteArray): ByteArray {
        val len = payload.size
        return byteArrayOf(
            ((msgType shr 8) and 0xFF).toByte(),
            (msgType and 0xFF).toByte(),
            ((len shr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte()
        ) + payload
    }

    // Wraps an already-encrypted ciphertext in the outer noise frame: \x01 + uint16_BE(len)
    fun buildNoiseOuterFrame(ciphertext: ByteArray): ByteArray {
        val len = ciphertext.size
        return byteArrayOf(
            0x01,
            ((len shr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte()
        ) + ciphertext
    }

    // Parses one noise outer frame from the stream: reads header byte + 2-byte length,
    // then reads exactly that many bytes (the ciphertext, to be decrypted by caller).
    fun readNoiseOuterFrame(input: InputStream): ByteArray {
        val header = input.readExactly(3)
        require(header[0] == 0x01.toByte()) {
            "Expected noise frame marker 0x01, got 0x${header[0].toUByte().toString(16)}"
        }
        val frameLen = ((header[1].toInt() and 0xFF) shl 8) or (header[2].toInt() and 0xFF)
        return input.readExactly(frameLen)
    }

    // Parses type + payload from a decrypted noise plaintext (4+ bytes).
    fun parseNoisePlaintext(msg: ByteArray): Pair<Int, ByteArray> {
        require(msg.size >= 4) { "Decrypted noise message too short: ${msg.size} bytes" }
        val msgType = ((msg[0].toInt() and 0xFF) shl 8) or (msg[1].toInt() and 0xFF)
        val payload = msg.copyOfRange(4, msg.size)
        return Pair(msgType, payload)
    }

    // --- Message encoders ---

    fun encodeHelloRequest(clientInfo: String = "DarkroomTimer"): ByteArray {
        val pb = ProtoBuilder()
        pb.string(1, clientInfo)
        pb.varint(2, 1L)   // api_version_major
        pb.varint(3, 14L)  // api_version_minor
        return pb.toByteArray()
    }

    fun encodePingRequest(): ByteArray = byteArrayOf()

    fun encodePingResponse(): ByteArray = byteArrayOf()

    fun encodeListEntitiesRequest(): ByteArray = byteArrayOf()

    fun encodeDisconnectRequest(): ByteArray = byteArrayOf()

    fun encodeDisconnectResponse(): ByteArray = byteArrayOf()

    // Encodes a SwitchCommandRequest: key=fixed32, state=bool
    fun encodeSwitchCommand(key: Int, state: Boolean): ByteArray {
        val pb = ProtoBuilder()
        pb.fixed32(1, key)
        pb.varint(2, if (state) 1L else 0L)
        return pb.toByteArray()
    }

    // --- Message decoders ---

    data class HelloResponse(val apiVersionMajor: Int, val apiVersionMinor: Int, val serverInfo: String)

    fun decodeHelloResponse(payload: ByteArray): HelloResponse {
        val fields = ProtoParser(payload).readAll()
        val major = (fields[1]?.firstOrNull() as? Long)?.toInt() ?: 0
        val minor = (fields[2]?.firstOrNull() as? Long)?.toInt() ?: 0
        val info = (fields[3]?.firstOrNull() as? ByteArray)?.toString(Charsets.UTF_8) ?: ""
        return HelloResponse(major, minor, info)
    }

    data class SwitchEntity(val objectId: String, val key: Int, val name: String)

    fun decodeSwitchEntity(payload: ByteArray): SwitchEntity {
        val fields = ProtoParser(payload).readAll()
        val objectId = (fields[1]?.firstOrNull() as? ByteArray)?.toString(Charsets.UTF_8) ?: ""
        val key = (fields[2]?.firstOrNull() as? Int) ?: 0  // fixed32 → Int
        val name = (fields[3]?.firstOrNull() as? ByteArray)?.toString(Charsets.UTF_8) ?: ""
        return SwitchEntity(objectId, key, name)
    }

    // --- Protobuf encoder ---

    private class ProtoBuilder {
        private val buf = mutableListOf<Byte>()

        fun varint(fieldNum: Int, value: Long) {
            writeTag(fieldNum, 0)
            writeVarint(value)
        }

        fun string(fieldNum: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeTag(fieldNum, 2)
            writeVarint(bytes.size.toLong())
            bytes.forEach { buf.add(it) }
        }

        fun fixed32(fieldNum: Int, value: Int) {
            writeTag(fieldNum, 5)
            val bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
            bb.array().forEach { buf.add(it) }
        }

        private fun writeTag(fieldNum: Int, wireType: Int) = writeVarint(((fieldNum shl 3) or wireType).toLong())

        private fun writeVarint(value: Long) {
            var v = value
            while (v and 0x7F.toLong().inv() != 0L) {
                buf.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            buf.add((v and 0x7F).toByte())
        }

        fun toByteArray() = buf.toByteArray()
    }

    // --- Protobuf parser ---
    // Returns a map of field_number → list of values.
    // Values: Long for varint (wire 0), ByteArray for length-delimited (wire 2), Int for fixed32 (wire 5).

    private class ProtoParser(private val data: ByteArray) {
        private var pos = 0

        fun readAll(): Map<Int, List<Any>> {
            val result = mutableMapOf<Int, MutableList<Any>>()
            while (pos < data.size) {
                val tag = readVarint().toInt()
                val fieldNum = tag ushr 3
                val wireType = tag and 0x07
                val value: Any = when (wireType) {
                    0 -> readVarint()
                    2 -> {
                        val len = readVarint().toInt()
                        val bytes = data.copyOfRange(pos, pos + len)
                        pos += len
                        bytes
                    }
                    5 -> {
                        val bb = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.LITTLE_ENDIAN)
                        pos += 4
                        bb.int
                    }
                    else -> { skipUnknown(wireType); continue }
                }
                result.getOrPut(fieldNum) { mutableListOf() }.add(value)
            }
            return result
        }

        private fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val b = data[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return result
        }

        private fun skipUnknown(wireType: Int) {
            when (wireType) {
                1 -> pos += 8  // 64-bit
                5 -> pos += 4  // 32-bit
            }
        }
    }
}

// Reads exactly n bytes from this InputStream; throws EOFException if stream ends early.
internal fun InputStream.readExactly(n: Int): ByteArray {
    val buf = ByteArray(n)
    var off = 0
    while (off < n) {
        val read = read(buf, off, n - off)
        if (read == -1) throw EOFException("Expected $n bytes, got $off")
        off += read
    }
    return buf
}
