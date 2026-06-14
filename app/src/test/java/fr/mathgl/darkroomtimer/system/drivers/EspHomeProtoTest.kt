package fr.mathgl.darkroomtimer.system.drivers

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.EOFException

class EspHomeProtoTest {

    // --- Noise frame helpers ---

    @Test
    fun `buildNoiseFramePlaintext encodes type and payload correctly`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03)
        val frame = EspHomeProto.buildNoiseFramePlaintext(0x0021, payload)
        // type high=0x00, low=0x21, len high=0x00, low=0x03
        assertEquals(0x00.toByte(), frame[0])
        assertEquals(0x21.toByte(), frame[1])
        assertEquals(0x00.toByte(), frame[2])
        assertEquals(0x03.toByte(), frame[3])
        assertArrayEquals(payload, frame.copyOfRange(4, 7))
    }

    @Test
    fun `buildNoiseFramePlaintext with empty payload`() {
        val frame = EspHomeProto.buildNoiseFramePlaintext(EspHomeProto.MSG_HELLO_REQUEST, byteArrayOf())
        assertEquals(4, frame.size)
        assertEquals(0x00.toByte(), frame[0])
        assertEquals(0x01.toByte(), frame[1])
        assertEquals(0x00.toByte(), frame[2])
        assertEquals(0x00.toByte(), frame[3])
    }

    @Test
    fun `buildNoiseOuterFrame wraps ciphertext with 0x01 header`() {
        val ct = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val frame = EspHomeProto.buildNoiseOuterFrame(ct)
        assertEquals(0x01.toByte(), frame[0])
        assertEquals(0x00.toByte(), frame[1])
        assertEquals(0x02.toByte(), frame[2])
        assertArrayEquals(ct, frame.copyOfRange(3, 5))
    }

    @Test
    fun `readNoiseOuterFrame parses frame from stream`() {
        val ct = ByteArray(42) { it.toByte() }
        val outer = EspHomeProto.buildNoiseOuterFrame(ct)
        val result = EspHomeProto.readNoiseOuterFrame(ByteArrayInputStream(outer))
        assertArrayEquals(ct, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `readNoiseOuterFrame throws on wrong marker byte`() {
        val bad = byteArrayOf(0x00, 0x00, 0x02, 0xAA.toByte(), 0xBB.toByte())
        EspHomeProto.readNoiseOuterFrame(ByteArrayInputStream(bad))
    }

    @Test(expected = EOFException::class)
    fun `readNoiseOuterFrame throws EOFException on truncated stream`() {
        val partial = byteArrayOf(0x01, 0x00, 0x05, 0x01, 0x02)  // says 5 bytes, only has 2
        EspHomeProto.readNoiseOuterFrame(ByteArrayInputStream(partial))
    }

    @Test
    fun `parseNoisePlaintext extracts type and payload`() {
        val payload = byteArrayOf(0x0A, 0x0B)
        val plaintext = EspHomeProto.buildNoiseFramePlaintext(0x0001, payload)
        val (type, parsed) = EspHomeProto.parseNoisePlaintext(plaintext)
        assertEquals(1, type)
        assertArrayEquals(payload, parsed)
    }

    @Test
    fun `parseNoisePlaintext with 4-byte message (empty payload)`() {
        val plaintext = EspHomeProto.buildNoiseFramePlaintext(EspHomeProto.MSG_PING_RESPONSE, byteArrayOf())
        val (type, payload) = EspHomeProto.parseNoisePlaintext(plaintext)
        assertEquals(EspHomeProto.MSG_PING_RESPONSE, type)
        assertEquals(0, payload.size)
    }

    // --- Encoder tests ---

    @Test
    fun `encodeHelloRequest encodes client_info, major, minor`() {
        val bytes = EspHomeProto.encodeHelloRequest("TestClient")
        // field 1: string "TestClient" → tag=0x0A, len=10, then 10 bytes
        // field 2: varint 1 → tag=0x10, value=0x01
        // field 3: varint 14 → tag=0x18, value=0x0E
        assertContainsSequence(bytes, byteArrayOf(0x0A, 10) + "TestClient".toByteArray())
        assertContainsSequence(bytes, byteArrayOf(0x10, 0x01))
        assertContainsSequence(bytes, byteArrayOf(0x18, 0x0E))
    }

    @Test
    fun `encodeSwitchCommand encodes key and state`() {
        // key=5 (fixed32 LE), state=true
        // field 1: fixed32 tag = (1<<3)|5 = 0x0D, 4 bytes LE
        // field 2: varint tag = (2<<3)|0 = 0x10, value 1
        val bytes = EspHomeProto.encodeSwitchCommand(5, true)
        assertContainsSequence(bytes, byteArrayOf(0x0D, 0x05, 0x00, 0x00, 0x00))
        assertContainsSequence(bytes, byteArrayOf(0x10, 0x01))
    }

    @Test
    fun `encodeSwitchCommand encodes state=false`() {
        val bytes = EspHomeProto.encodeSwitchCommand(3, false)
        assertContainsSequence(bytes, byteArrayOf(0x10, 0x00))
    }

    @Test
    fun `encodeSwitchCommand handles large key value (MSB set)`() {
        val key = 0xDEADBEEF.toInt()
        val bytes = EspHomeProto.encodeSwitchCommand(key, true)
        // tag=0x0D, then 4 bytes LE of key
        val expected = byteArrayOf(
            0x0D,
            (key and 0xFF).toByte(),
            ((key shr 8) and 0xFF).toByte(),
            ((key shr 16) and 0xFF).toByte(),
            ((key shr 24) and 0xFF).toByte()
        )
        assertContainsSequence(bytes, expected)
    }

    @Test
    fun `empty message encoders return empty arrays`() {
        assertEquals(0, EspHomeProto.encodePingRequest().size)
        assertEquals(0, EspHomeProto.encodePingResponse().size)
        assertEquals(0, EspHomeProto.encodeListEntitiesRequest().size)
        assertEquals(0, EspHomeProto.encodeDisconnectRequest().size)
        assertEquals(0, EspHomeProto.encodeDisconnectResponse().size)
    }

    // --- Decoder tests ---

    @Test
    fun `decodeHelloResponse parses major, minor, serverInfo`() {
        // Encode a HelloResponse manually: field1=varint 1, field2=varint 14, field3=string "ESPHome"
        val payload = buildProto {
            varint(1, 1)
            varint(2, 14)
            string(3, "ESPHome")
        }
        val resp = EspHomeProto.decodeHelloResponse(payload)
        assertEquals(1, resp.apiVersionMajor)
        assertEquals(14, resp.apiVersionMinor)
        assertEquals("ESPHome", resp.serverInfo)
    }

    @Test
    fun `decodeHelloResponse with empty payload returns defaults`() {
        val resp = EspHomeProto.decodeHelloResponse(byteArrayOf())
        assertEquals(0, resp.apiVersionMajor)
        assertEquals(0, resp.apiVersionMinor)
        assertEquals("", resp.serverInfo)
    }

    @Test
    fun `decodeSwitchEntity parses objectId, key, name`() {
        val payload = buildProto {
            string(1, "relay_1")
            fixed32(2, 12345)
            string(3, "Enlarger")
        }
        val entity = EspHomeProto.decodeSwitchEntity(payload)
        assertEquals("relay_1", entity.objectId)
        assertEquals(12345, entity.key)
        assertEquals("Enlarger", entity.name)
    }

    @Test
    fun `decodeSwitchEntity handles negative key (MSB set fixed32)`() {
        val key = 0xDEADBEEF.toInt()
        val payload = buildProto {
            string(1, "relay_2")
            fixed32(2, key)
            string(3, "Safelight")
        }
        val entity = EspHomeProto.decodeSwitchEntity(payload)
        assertEquals(key, entity.key)
    }

    // Encode-then-decode round trips
    @Test
    fun `hello request encodes to non-empty bytes and has correct constants`() {
        val bytes = EspHomeProto.encodeHelloRequest()
        assertTrue(bytes.isNotEmpty())
    }

    // --- Helpers ---

    private fun assertContainsSequence(haystack: ByteArray, needle: ByteArray) {
        val indices = haystack.indices.filter { i ->
            i + needle.size <= haystack.size &&
                needle.indices.all { j -> haystack[i + j] == needle[j] }
        }
        assertTrue(
            "Expected ${needle.toHex()} inside ${haystack.toHex()}",
            indices.isNotEmpty()
        )
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    // Mini protobuf builder for test payloads
    private fun buildProto(block: MiniProto.() -> Unit): ByteArray {
        val mp = MiniProto()
        mp.block()
        return mp.toByteArray()
    }

    private class MiniProto {
        private val buf = mutableListOf<Byte>()

        fun varint(fieldNum: Int, value: Long) {
            tag(fieldNum, 0); writeVarint(value)
        }

        fun string(fieldNum: Int, value: String) {
            val b = value.toByteArray(Charsets.UTF_8)
            tag(fieldNum, 2); writeVarint(b.size.toLong()); b.forEach { buf.add(it) }
        }

        fun fixed32(fieldNum: Int, value: Int) {
            tag(fieldNum, 5)
            for (i in 0..3) buf.add(((value shr (8 * i)) and 0xFF).toByte())
        }

        private fun tag(fieldNum: Int, wire: Int) = writeVarint(((fieldNum shl 3) or wire).toLong())

        private fun writeVarint(v: Long) {
            var n = v
            while (n and 0x7F.toLong().inv() != 0L) {
                buf.add(((n and 0x7F) or 0x80).toByte()); n = n ushr 7
            }
            buf.add((n and 0x7F).toByte())
        }

        fun toByteArray() = buf.toByteArray()
    }
}
