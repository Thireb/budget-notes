package com.budgetnotes.app.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCrypto {
    private const val PBKDF2_ITERATIONS = 310_000
    private const val KEY_BYTES = 32
    private const val SALT_BYTES = 16
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val IMAGE_INFO = "card-images-v1"

    fun generateSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun deriveKey(pin: CharArray, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, KEY_BYTES * 8)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Verification blob: SHA-256(key) — never store the PIN. */
    fun keyVerifier(key: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(key)

    fun keysMatch(key: ByteArray, verifier: ByteArray): Boolean {
        val actual = keyVerifier(key)
        if (actual.size != verifier.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor verifier[i].toInt())
        return diff == 0
    }

    fun imageKey(dbKey: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(dbKey)
        md.update(IMAGE_INFO.toByteArray(Charsets.UTF_8))
        return md.digest()
    }

    fun encryptAesGcm(key: ByteArray, plaintext: ByteArray): ByteArray {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decryptAesGcm(key: ByteArray, blob: ByteArray): ByteArray {
        require(blob.size > GCM_IV_BYTES) { "ciphertext too short" }
        val iv = blob.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = blob.copyOfRange(GCM_IV_BYTES, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun toBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun fromBase64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    fun clear(bytes: ByteArray?) {
        bytes?.fill(0)
    }
}
