package fr.mathgl.darkroomtimer.system.drivers.noise

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Noise NNpsk0 protocol initiator for ESPHome Native API.
// Pattern: Noise_NNpsk0_25519_ChaChaPoly_SHA256
// Prologue: b"NoiseAPIInit" + 2 null bytes
// PSK inserted at position 0 of message 1 (before the 'e' token).
class NoiseNNpsk0(private val psk: ByteArray) {

    private var h: ByteArray = ByteArray(32)
    private var ck: ByteArray = ByteArray(32)
    private var cipherKey: ByteArray? = null
    private var handshakeNonce: Long = 0

    private var sendKey: ByteArray? = null
    private var recvKey: ByteArray? = null
    var sendNonce: Long = 0
        private set
    var recvNonce: Long = 0
        private set

    private val ephemeralPrivKey: PrivateKey
    private val ephemeralPubRaw: ByteArray  // 32-byte raw X25519 public key

    init {
        // InitializeSymmetric("Noise_NNpsk0_25519_ChaChaPoly_SHA256")
        h = sha256("Noise_NNpsk0_25519_ChaChaPoly_SHA256".toByteArray(Charsets.US_ASCII))
        ck = h.clone()
        // MixHash(prologue) — ESPHome prologue is "NoiseAPIInit" + 0x00 0x00 (14 bytes)
        val prologue = "NoiseAPIInit".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x00, 0x00)
        mixHash(prologue)
        // Generate ephemeral X25519 keypair
        val kpg = KeyPairGenerator.getInstance("X25519")
        val kp = kpg.generateKeyPair()
        ephemeralPrivKey = kp.private
        ephemeralPubRaw = kp.public.encoded.takeLast(32).toByteArray()
    }

    // Returns the 49-byte client handshake payload: [0x00] + ephemeral_pub[32] + mac[16]
    // Internally processes the NNpsk0 message 1 tokens: psk, e, then EncryptAndHash(b"")
    fun buildClientHandshakePayload(): ByteArray {
        // Token psk (psk0 modifier — before any other tokens in message 1)
        mixKeyAndHash(psk)
        // Token e: MixHash(pub) then, per Noise PSK extension, also MixKey(pub)
        // MixKey overwrites the PSK-derived cipher key with the ephemeral-derived key.
        mixHash(ephemeralPubRaw)
        mixKey(ephemeralPubRaw)
        // EncryptAndHash(b""): Noise spec requires encrypting the (empty) payload after all tokens
        val mac = encryptAndHash(byteArrayOf())
        return byteArrayOf(0x00) + ephemeralPubRaw + mac
    }

    // Processes NNpsk0 message 2: e, ee, then DecryptAndHash(mac)
    // serverHandshakePayload = server-sent bytes starting after the preamble (0x00) byte.
    // Expected content: server_ephemeral_pub[32] + server_mac[16]
    fun processServerHandshakePayload(serverHandshakePayload: ByteArray) {
        require(serverHandshakePayload.size >= 48) {
            "Server handshake payload too short: ${serverHandshakePayload.size}"
        }
        val serverPubRaw = serverHandshakePayload.copyOf(32)
        val serverMac = serverHandshakePayload.copyOfRange(32, 48)
        // Token e (server): MixHash(pub) then, per Noise PSK extension, also MixKey(pub)
        mixHash(serverPubRaw)
        mixKey(serverPubRaw)
        // Token ee: DH(e, server_e) → MixKey (also resets handshakeNonce)
        val dhResult = dh(ephemeralPrivKey, serverPubRaw)
        mixKey(dhResult)
        // DecryptAndHash(mac): verify server's Poly1305 tag and update h
        decryptAndHash(serverMac)
        // Split into send/recv cipher states
        split()
    }

    // Encrypts a post-handshake message using the send key.
    fun encryptMessage(plaintext: ByteArray): ByteArray {
        val k = sendKey ?: error("Handshake not complete — sendKey is null")
        val ct = ChaCha20Poly1305.encrypt(k, buildNonce(sendNonce), plaintext)
        sendNonce++
        return ct
    }

    // Decrypts a post-handshake message using the recv key.
    fun decryptMessage(ciphertext: ByteArray): ByteArray {
        val k = recvKey ?: error("Handshake not complete — recvKey is null")
        val pt = ChaCha20Poly1305.decrypt(k, buildNonce(recvNonce), ciphertext)
        recvNonce++
        return pt
    }

    // Noise EncryptAndHash: encrypt plaintext with cipherKey (h as AD), then MixHash(ciphertext)
    private fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val k = cipherKey ?: run { mixHash(plaintext); return plaintext }
        val ad = h.clone()
        val ciphertext = ChaCha20Poly1305.encrypt(k, buildNonce(handshakeNonce++), plaintext, ad)
        mixHash(ciphertext)
        return ciphertext
    }

    // Noise DecryptAndHash: verify+decrypt ciphertext with cipherKey (h as AD), then MixHash(ciphertext)
    private fun decryptAndHash(ciphertext: ByteArray): ByteArray {
        val k = cipherKey ?: run { mixHash(ciphertext); return ciphertext }
        val ad = h.clone()
        val plaintext = ChaCha20Poly1305.decrypt(k, buildNonce(handshakeNonce++), ciphertext, ad)
        mixHash(ciphertext)
        return plaintext
    }

    // Noise primitive: h = SHA256(h || data)
    private fun mixHash(data: ByteArray) {
        h = sha256(h + data)
    }

    // Noise primitive: (ck, cipherKey) = HKDF2(ck, dh_result); resets handshake nonce
    private fun mixKey(dhResult: ByteArray) {
        val (newCk, tempK) = hkdf2(ck, dhResult)
        ck = newCk
        cipherKey = tempK
        handshakeNonce = 0
    }

    // Noise primitive for psk token: (ck, tempH, cipherKey) = HKDF3(ck, psk); MixHash(tempH)
    private fun mixKeyAndHash(input: ByteArray) {
        val (newCk, tempH, tempK) = hkdf3(ck, input)
        ck = newCk
        mixHash(tempH)
        cipherKey = tempK
        handshakeNonce = 0
    }

    // Derive send/recv session keys from the chaining key.
    // sendKey = c1 (initiator→responder), recvKey = c2 (responder→initiator)
    private fun split() {
        val (c1, c2) = hkdf2(ck, ByteArray(0))
        sendKey = c1
        recvKey = c2
        sendNonce = 0
        recvNonce = 0
    }

    // HKDF-SHA256: extract-expand, 2 outputs of 32 bytes each
    private fun hkdf2(salt: ByteArray, ikm: ByteArray): Pair<ByteArray, ByteArray> {
        val prk = hmacSha256(salt, ikm)
        val t1 = hmacSha256(prk, byteArrayOf(0x01))
        val t2 = hmacSha256(prk, t1 + byteArrayOf(0x02))
        return Pair(t1, t2)
    }

    // HKDF-SHA256: extract-expand, 3 outputs of 32 bytes each
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

    // Nonce format for ChaCha20-Poly1305 in ESPHome: struct.pack("<LQ", 0, counter)
    // = 4 bytes zero (LE uint32) + 8 bytes counter (LE uint64) = 12 bytes total
    private fun buildNonce(counter: Long): ByteArray =
        ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(0).putLong(counter).array()

    // X25519 DH: returns 32-byte shared secret
    private fun dh(priv: PrivateKey, serverPubRaw: ByteArray): ByteArray {
        val serverPub = rawToX25519PublicKey(serverPubRaw)
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(priv)
        ka.doPhase(serverPub, true)
        return ka.generateSecret()
    }

    // Wrap 32 raw bytes into a JCE X25519 PublicKey using SubjectPublicKeyInfo DER encoding.
    // DER header for X25519 (OID 1.3.101.110 = 2B 65 6E):
    //   30 2A 30 05 06 03 2B 65 6E 03 21 00 <32 bytes>
    private fun rawToX25519PublicKey(raw: ByteArray): PublicKey {
        require(raw.size == 32) { "X25519 raw public key must be 32 bytes" }
        val der = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
        ) + raw
        return KeyFactory.getInstance("X25519").generatePublic(X509EncodedKeySpec(der))
    }
}
