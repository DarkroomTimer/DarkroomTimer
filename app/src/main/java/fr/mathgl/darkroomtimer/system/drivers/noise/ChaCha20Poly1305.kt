package fr.mathgl.darkroomtimer.system.drivers.noise

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

// ChaCha20 stream cipher — RFC 8439
internal object ChaCha20 {

    private val CONSTANTS = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)

    fun block(key: ByteArray, nonce: ByteArray, counter: Int): ByteArray {
        require(key.size == 32) { "Key must be 32 bytes" }
        require(nonce.size == 12) { "Nonce must be 12 bytes" }

        val state = IntArray(16)
        state[0] = CONSTANTS[0]; state[1] = CONSTANTS[1]
        state[2] = CONSTANTS[2]; state[3] = CONSTANTS[3]

        val keyBuf = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0..7) state[4 + i] = keyBuf.getInt()

        state[12] = counter

        val nonceBuf = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN)
        state[13] = nonceBuf.getInt()
        state[14] = nonceBuf.getInt()
        state[15] = nonceBuf.getInt()

        val working = state.copyOf()
        repeat(10) {
            // Column rounds
            quarterRound(working, 0, 4, 8, 12)
            quarterRound(working, 1, 5, 9, 13)
            quarterRound(working, 2, 6, 10, 14)
            quarterRound(working, 3, 7, 11, 15)
            // Diagonal rounds
            quarterRound(working, 0, 5, 10, 15)
            quarterRound(working, 1, 6, 11, 12)
            quarterRound(working, 2, 7, 8, 13)
            quarterRound(working, 3, 4, 9, 14)
        }
        for (i in 0..15) working[i] += state[i]

        val out = ByteArray(64)
        val outBuf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        for (w in working) outBuf.putInt(w)
        return out
    }

    private fun quarterRound(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
        s[a] += s[b]; s[d] = s[d] xor s[a]; s[d] = s[d] rotateLeft 16
        s[c] += s[d]; s[b] = s[b] xor s[c]; s[b] = s[b] rotateLeft 12
        s[a] += s[b]; s[d] = s[d] xor s[a]; s[d] = s[d] rotateLeft 8
        s[c] += s[d]; s[b] = s[b] xor s[c]; s[b] = s[b] rotateLeft 7
    }

    private infix fun Int.rotateLeft(n: Int) = (this shl n) or (this ushr (32 - n))
}

// Poly1305 MAC — RFC 8439. Uses BigInteger for 130-bit arithmetic.
internal object Poly1305 {

    private val P = BigInteger.ONE.shiftLeft(130).subtract(BigInteger.valueOf(5)) // 2^130 - 5
    private val R_MASK = BigInteger("0ffffffc0ffffffc0ffffffc0fffffff", 16)

    fun mac(key: ByteArray, message: ByteArray): ByteArray {
        require(key.size == 32) { "Poly1305 key must be 32 bytes" }

        val rBytes = key.copyOf(16)
        clamp(rBytes)
        val r = rBytes.toLittleEndianBigInteger()
        val s = key.copyOfRange(16, 32).toLittleEndianBigInteger()

        var acc = BigInteger.ZERO
        var offset = 0
        while (offset < message.size) {
            val remaining = message.size - offset
            val chunkLen = minOf(16, remaining)
            val block = ByteArray(chunkLen + 1)
            message.copyInto(block, 0, offset, offset + chunkLen)
            block[chunkLen] = 0x01  // appended bit
            val n = block.toLittleEndianBigInteger()
            acc = acc.add(n).multiply(r).mod(P)
            offset += chunkLen
        }
        acc = acc.add(s)

        val tag = ByteArray(16)
        val tagBytes = acc.toByteArray()
        // BigInteger.toByteArray() is big-endian; we need little-endian 16 bytes
        for (i in 0 until minOf(16, tagBytes.size)) {
            tag[i] = tagBytes[tagBytes.size - 1 - i]
        }
        return tag
    }

    private fun clamp(r: ByteArray) {
        r[3] = (r[3].toInt() and 0x0F).toByte()
        r[7] = (r[7].toInt() and 0x0F).toByte()
        r[11] = (r[11].toInt() and 0x0F).toByte()
        r[15] = (r[15].toInt() and 0x0F).toByte()
        r[4] = (r[4].toInt() and 0xFC).toByte()
        r[8] = (r[8].toInt() and 0xFC).toByte()
        r[12] = (r[12].toInt() and 0xFC).toByte()
    }

    private fun ByteArray.toLittleEndianBigInteger(): BigInteger {
        val reversed = copyOf()
        reversed.reverse()
        return BigInteger(1, reversed)
    }
}

// ChaCha20-Poly1305 AEAD — RFC 8439 section 2.8
object ChaCha20Poly1305 {

    fun encrypt(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray = byteArrayOf()): ByteArray {
        val keystream = generateKeystream(key, nonce, plaintext.size)
        val ciphertext = ByteArray(plaintext.size) { (plaintext[it].toInt() xor keystream[it].toInt()).toByte() }
        val tag = computeTag(key, nonce, aad, ciphertext)
        return ciphertext + tag
    }

    fun decrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray = byteArrayOf()): ByteArray {
        require(ciphertext.size >= 16) { "Ciphertext too short to contain MAC tag" }
        val ct = ciphertext.copyOf(ciphertext.size - 16)
        val tag = ciphertext.copyOfRange(ciphertext.size - 16, ciphertext.size)
        val expectedTag = computeTag(key, nonce, aad, ct)
        if (!constantTimeEquals(tag, expectedTag)) {
            throw IllegalArgumentException("ChaCha20-Poly1305: MAC verification failed")
        }
        val keystream = generateKeystream(key, nonce, ct.size)
        return ByteArray(ct.size) { (ct[it].toInt() xor keystream[it].toInt()).toByte() }
    }

    private fun generateKeystream(key: ByteArray, nonce: ByteArray, length: Int): ByteArray {
        val out = ByteArray(length)
        var counter = 1  // counter 0 is reserved for Poly1305 OTP key
        var offset = 0
        while (offset < length) {
            val block = ChaCha20.block(key, nonce, counter++)
            val copy = minOf(64, length - offset)
            block.copyInto(out, offset, 0, copy)
            offset += copy
        }
        return out
    }

    private fun computeTag(key: ByteArray, nonce: ByteArray, aad: ByteArray, ciphertext: ByteArray): ByteArray {
        val otpBlock = ChaCha20.block(key, nonce, 0)
        val otpKey = otpBlock.copyOf(32)
        val macInput = buildMacInput(aad, ciphertext)
        return Poly1305.mac(otpKey, macInput)
    }

    private fun buildMacInput(aad: ByteArray, ciphertext: ByteArray): ByteArray {
        val buf = ByteArrayBuilder()
        buf.append(aad)
        buf.padTo16()
        buf.append(ciphertext)
        buf.padTo16()
        buf.appendLE64(aad.size.toLong())
        buf.appendLE64(ciphertext.size.toLong())
        return buf.toByteArray()
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private class ByteArrayBuilder {
        private val data = mutableListOf<Byte>()

        fun append(bytes: ByteArray) { bytes.forEach { data.add(it) } }

        fun padTo16() {
            val rem = data.size % 16
            if (rem != 0) repeat(16 - rem) { data.add(0) }
        }

        fun appendLE64(value: Long) {
            for (i in 0..7) data.add((value ushr (8 * i)).toByte())
        }

        fun toByteArray() = data.toByteArray()
    }
}
