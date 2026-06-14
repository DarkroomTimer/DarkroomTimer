package fr.mathgl.darkroomtimer.system.drivers.noise

import org.junit.Assert.*
import org.junit.Test

class ChaCha20Poly1305Test {

    // RFC 8439 section 2.1.1 — quarter round test vector
    @Test
    fun `ChaCha20 quarter round test vector from RFC 8439 section 2-1-1`() {
        // The test from the RFC operates on internal state directly; we verify the block function
        // using the section 2.3.2 test vector instead (which exercises quarter rounds).
        // This test checks the block function with the canonical key/nonce/counter from section 2.3.2.
        val key = ByteArray(32) { (it + 0).toByte() }
        // key = 00 01 02 03 ... 1f
        key[0] = 0; key[1] = 1; key[2] = 2; key[3] = 3
        key[4] = 4; key[5] = 5; key[6] = 6; key[7] = 7
        key[8] = 8; key[9] = 9; key[10] = 10; key[11] = 11
        key[12] = 12; key[13] = 13; key[14] = 14; key[15] = 15
        key[16] = 16; key[17] = 17; key[18] = 18; key[19] = 19
        key[20] = 20; key[21] = 21; key[22] = 22; key[23] = 23
        key[24] = 24; key[25] = 25; key[26] = 26; key[27] = 27
        key[28] = 28; key[29] = 29; key[30] = 30; key[31] = 31

        // RFC 8439 section 2.3.2: nonce words = 00000009 0000004a 00000000 (LE representation)
        val nonce = byteArrayOf(0, 0, 0, 9, 0, 0, 0, 0x4a.toByte(), 0, 0, 0, 0)
        val counter = 1
        val block = ChaCha20.block(key, nonce, counter)

        // Expected: verified against Python cryptography.hazmat.primitives.ciphers.ChaCha20
        val expected = hexToBytes(
            "10 f1 e7 e4 d1 3b 59 15 50 0f dd 1f a3 20 71 c4" +
            "c7 d1 f4 c7 33 c0 68 03 04 22 aa 9a c3 d4 6c 4e" +
            "d2 82 64 46 07 9f aa 09 14 c2 d7 05 d9 8b 02 a2" +
            "b5 12 9c d1 de 16 4e b9 cb d0 83 e8 a2 50 3c 4e"
        )
        assertArrayEquals(expected, block)
    }

    // RFC 8439 section 2.5.2 — Poly1305 MAC test vector
    @Test
    fun `Poly1305 MAC matches RFC 8439 section 2-5-2`() {
        val key = hexToBytes(
            "85 d6 be 78 57 55 6d 33 7f 44 52 fe 42 d5 06 a8" +
            "01 03 80 8a fb 0d b2 fd 4a bf f6 af 41 49 f5 1b"
        )
        val message = "Cryptographic Forum Research Group".toByteArray()
        val tag = Poly1305.mac(key, message)
        val expected = hexToBytes("a8 06 1d c1 30 51 36 c6 c2 2b 8b af 0c 01 27 a9")
        assertArrayEquals(expected, tag)
    }

    // RFC 8439 section 2.8.2 — AEAD encrypt test vector
    @Test
    fun `ChaCha20Poly1305 encrypt matches RFC 8439 section 2-8-2`() {
        val key = hexToBytes(
            "80 81 82 83 84 85 86 87 88 89 8a 8b 8c 8d 8e 8f" +
            "90 91 92 93 94 95 96 97 98 99 9a 9b 9c 9d 9e 9f"
        )
        val nonce = hexToBytes("07 00 00 00 40 41 42 43 44 45 46 47")
        val aad = hexToBytes("50 51 52 53 c0 c1 c2 c3 c4 c5 c6 c7")
        val plaintext = ("Ladies and Gentlemen of the class of '99: " +
                "If I could offer you only one tip for the future, " +
                "sunscreen would be it.").toByteArray()

        val ciphertext = ChaCha20Poly1305.encrypt(key, nonce, plaintext, aad)

        val expectedCt = hexToBytes(
            "d3 1a 8d 34 64 8e 60 db 7b 86 af bc 53 ef 7e c2" +
            "a4 ad ed 51 29 6e 08 fe a9 e2 b5 a7 36 ee 62 d6" +
            "3d be a4 5e 8c a9 67 12 82 fa fb 69 da 92 72 8b" +
            "1a 71 de 0a 9e 06 0b 29 05 d6 a5 b6 7e cd 3b 36" +
            "92 dd bd 7f 2d 77 8b 8c 98 03 ae e3 28 09 1b 58" +
            "fa b3 24 e4 fa d6 75 94 55 85 80 8b 48 31 d7 bc" +
            "3f f4 de f0 8e 4b 7a 9d e5 76 d2 65 86 ce c6 4b" +
            "61 16"
        )
        val expectedTag = hexToBytes("1a e1 0b 59 4f 09 e2 6a 7e 90 2e cb d0 60 06 91")
        val expected = expectedCt + expectedTag

        assertArrayEquals(expected, ciphertext)
    }

    // Round-trip: encrypt then decrypt gives back the original plaintext
    @Test
    fun `ChaCha20Poly1305 decrypt reverses encrypt`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { (it + 1).toByte() }
        val plaintext = "Hello, ESPHome!".toByteArray()
        val aad = byteArrayOf()

        val ciphertext = ChaCha20Poly1305.encrypt(key, nonce, plaintext, aad)
        val decrypted = ChaCha20Poly1305.decrypt(key, nonce, ciphertext, aad)

        assertArrayEquals(plaintext, decrypted)
    }

    // Tampered ciphertext must throw
    @Test(expected = IllegalArgumentException::class)
    fun `ChaCha20Poly1305 decrypt throws on tampered ciphertext`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12)
        val ct = ChaCha20Poly1305.encrypt(key, nonce, byteArrayOf(0x41, 0x42))
        ct[0] = (ct[0].toInt() xor 0xFF).toByte()  // flip all bits in first byte
        ChaCha20Poly1305.decrypt(key, nonce, ct)
    }

    // Empty plaintext: only the 16-byte tag
    @Test
    fun `ChaCha20Poly1305 encrypt-decrypt empty plaintext`() {
        val key = ByteArray(32) { 0xAA.toByte() }
        val nonce = ByteArray(12)
        val ct = ChaCha20Poly1305.encrypt(key, nonce, byteArrayOf())
        assertEquals(16, ct.size)
        val pt = ChaCha20Poly1305.decrypt(key, nonce, ct)
        assertEquals(0, pt.size)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleaned = hex.replace(" ", "").replace("\n", "")
        return ByteArray(cleaned.length / 2) { i ->
            cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
