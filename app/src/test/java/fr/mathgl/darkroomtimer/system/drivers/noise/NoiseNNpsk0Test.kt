package fr.mathgl.darkroomtimer.system.drivers.noise

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class NoiseNNpsk0Test {

    // Self-consistency: initiator and responder complete the handshake and can exchange messages.
    @Test
    fun `NNpsk0 handshake self-consistency with valid PSK`() {
        val psk = ByteArray(32) { it.toByte() }
        val initiator = NoiseNNpsk0(psk)
        val responder = NoiseNNpsk0Responder(psk)

        // Initiator → Responder: [0x00] + ephPub[32] + mac[16]
        val clientPayload = initiator.buildClientHandshakePayload()
        assertEquals(49, clientPayload.size)
        assertEquals(0x00.toByte(), clientPayload[0])

        // Responder processes client payload (48 bytes: pub + mac), sends its own (48 bytes: pub + mac)
        val serverPayload = responder.processClientHello(clientPayload.copyOfRange(1, clientPayload.size))
        assertEquals(48, serverPayload.size)

        // Initiator processes server payload → split
        initiator.processServerHandshakePayload(serverPayload)

        // Both sides can now encrypt/decrypt
        val message = "Hello from initiator!".toByteArray()
        val ciphertext = initiator.encryptMessage(message)
        val plaintext = responder.decryptFromInitiator(ciphertext)
        assertArrayEquals(message, plaintext)

        // Reverse direction
        val response = "Hello from responder!".toByteArray()
        val responseCt = responder.encryptToInitiator(response)
        val responsePt = initiator.decryptMessage(responseCt)
        assertArrayEquals(response, responsePt)
    }

    // Multiple messages: nonces increment correctly on both sides
    @Test
    fun `NNpsk0 multiple messages in sequence`() {
        val psk = ByteArray(32) { 0xAB.toByte() }
        val initiator = NoiseNNpsk0(psk)
        val responder = NoiseNNpsk0Responder(psk)

        val clientPayload = initiator.buildClientHandshakePayload()
        val serverPayload = responder.processClientHello(clientPayload.copyOfRange(1, clientPayload.size))
        initiator.processServerHandshakePayload(serverPayload)

        for (i in 0..9) {
            val msg = "Message $i".toByteArray()
            val ct = initiator.encryptMessage(msg)
            val pt = responder.decryptFromInitiator(ct)
            assertArrayEquals("Message $i roundtrip failed", msg, pt)
        }
    }

    // Wrong PSK: responder rejects client handshake MAC immediately
    @Test(expected = IllegalArgumentException::class)
    fun `NNpsk0 handshake fails with wrong PSK on responder side`() {
        val psk1 = ByteArray(32) { 0x01 }
        val psk2 = ByteArray(32) { 0x02 }
        val initiator = NoiseNNpsk0(psk1)
        val responder = NoiseNNpsk0Responder(psk2)

        val clientPayload = initiator.buildClientHandshakePayload()
        // With correct Noise EncryptAndHash, PSK mismatch causes MAC verification to fail
        // immediately in processClientHello — IllegalArgumentException expected here
        responder.processClientHello(clientPayload.copyOfRange(1, clientPayload.size))
    }
}

// Test helper: responder role for NNpsk0 handshake (ESPHome device side)
private class NoiseNNpsk0Responder(psk: ByteArray) {
    private var h: ByteArray
    private var ck: ByteArray
    private var cipherKey: ByteArray? = null
    private var handshakeNonce: Long = 0

    private var sendKey: ByteArray? = null
    private var recvKey: ByteArray? = null
    private var sendNonce: Long = 0
    private var recvNonce: Long = 0

    private val ephemeralPrivKey: java.security.PrivateKey
    private val ephemeralPubRaw: ByteArray

    init {
        h = sha256("Noise_NNpsk0_25519_ChaChaPoly_SHA256".toByteArray(Charsets.US_ASCII))
        ck = h.clone()
        val prologue = "NoiseAPIInit".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00, 0x00)
        mixHash(prologue)
        // Process psk0 token (responder also processes psk at position 0)
        mixKeyAndHash(psk)
        val kpg = KeyPairGenerator.getInstance("X25519")
        val kp = kpg.generateKeyPair()
        ephemeralPrivKey = kp.private
        ephemeralPubRaw = kp.public.encoded.takeLast(32).toByteArray()
    }

    // Process client handshake payload (48 bytes: pub[32] + mac[16]).
    // Returns server handshake payload (48 bytes: pub[32] + mac[16]).
    fun processClientHello(clientPayload: ByteArray): ByteArray {
        require(clientPayload.size >= 48) { "Client payload too short: ${clientPayload.size}" }
        val clientPubRaw = clientPayload.copyOf(32)
        val clientMac = clientPayload.copyOfRange(32, 48)
        // Token e (client): MixHash(pub) then per Noise PSK extension MixKey(pub)
        mixHash(clientPubRaw)
        mixKey(clientPubRaw)
        // DecryptAndHash: verify client's Poly1305 tag
        decryptAndHash(clientMac)
        // Responder's token e: MixHash(pub) then per Noise PSK extension MixKey(pub)
        mixHash(ephemeralPubRaw)
        mixKey(ephemeralPubRaw)
        // Token ee: DH(responder_e, client_e) → MixKey (resets handshakeNonce)
        val dhResult = dh(ephemeralPrivKey, clientPubRaw)
        mixKey(dhResult)
        // EncryptAndHash: produce server's Poly1305 tag
        val serverMac = encryptAndHash(byteArrayOf())
        // Split
        split()
        return ephemeralPubRaw + serverMac
    }

    fun decryptFromInitiator(ciphertext: ByteArray): ByteArray {
        val k = recvKey ?: error("Not ready")
        val pt = ChaCha20Poly1305.decrypt(k, buildNonce(recvNonce), ciphertext)
        recvNonce++
        return pt
    }

    fun encryptToInitiator(plaintext: ByteArray): ByteArray {
        val k = sendKey ?: error("Not ready")
        val ct = ChaCha20Poly1305.encrypt(k, buildNonce(sendNonce), plaintext)
        sendNonce++
        return ct
    }

    private fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val k = cipherKey ?: run { mixHash(plaintext); return plaintext }
        val ad = h.clone()
        val ciphertext = ChaCha20Poly1305.encrypt(k, buildNonce(handshakeNonce++), plaintext, ad)
        mixHash(ciphertext)
        return ciphertext
    }

    private fun decryptAndHash(ciphertext: ByteArray): ByteArray {
        val k = cipherKey ?: run { mixHash(ciphertext); return ciphertext }
        val ad = h.clone()
        val plaintext = ChaCha20Poly1305.decrypt(k, buildNonce(handshakeNonce++), ciphertext, ad)
        mixHash(ciphertext)
        return plaintext
    }

    private fun mixHash(data: ByteArray) { h = sha256(h + data) }

    private fun mixKey(dhResult: ByteArray) {
        val (newCk, tempK) = hkdf2(ck, dhResult)
        ck = newCk
        cipherKey = tempK
        handshakeNonce = 0
    }

    private fun mixKeyAndHash(input: ByteArray) {
        val (newCk, tempH, tempK) = hkdf3(ck, input)
        ck = newCk
        mixHash(tempH)
        cipherKey = tempK
        handshakeNonce = 0
    }

    private fun split() {
        // Responder's split: c1 = recvKey (from initiator), c2 = sendKey (to initiator)
        val (c1, c2) = hkdf2(ck, ByteArray(0))
        recvKey = c1
        sendKey = c2
        recvNonce = 0
        sendNonce = 0
    }

    private fun hkdf2(salt: ByteArray, ikm: ByteArray): Pair<ByteArray, ByteArray> {
        val prk = hmacSha256(salt, ikm)
        val t1 = hmacSha256(prk, byteArrayOf(0x01))
        val t2 = hmacSha256(prk, t1 + byteArrayOf(0x02))
        return Pair(t1, t2)
    }

    private fun hkdf3(salt: ByteArray, ikm: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        val prk = hmacSha256(salt, ikm)
        val t1 = hmacSha256(prk, byteArrayOf(0x01))
        val t2 = hmacSha256(prk, t1 + byteArrayOf(0x02))
        val t3 = hmacSha256(prk, t2 + byteArrayOf(0x03))
        return Triple(t1, t2, t3)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun buildNonce(counter: Long): ByteArray =
        ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(0).putLong(counter).array()

    private fun dh(priv: java.security.PrivateKey, pubRaw: ByteArray): ByteArray {
        val der = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
        ) + pubRaw
        val pub = KeyFactory.getInstance("X25519").generatePublic(X509EncodedKeySpec(der))
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(priv)
        ka.doPhase(pub, true)
        return ka.generateSecret()
    }
}
