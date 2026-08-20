package com.budgetnotes.app.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultCryptoTest {
    @Test
    fun deriveKey_isDeterministic() {
        val salt = ByteArray(16) { 1 }
        val a = VaultCrypto.deriveKey("123456".toCharArray(), salt)
        val b = VaultCrypto.deriveKey("123456".toCharArray(), salt)
        assertArrayEquals(a, b)
    }

    @Test
    fun keysMatch_detectsWrongPin() {
        val salt = VaultCrypto.generateSalt()
        val key = VaultCrypto.deriveKey("123456".toCharArray(), salt)
        val verifier = VaultCrypto.keyVerifier(key)
        assertTrue(VaultCrypto.keysMatch(key, verifier))
        val wrong = VaultCrypto.deriveKey("000000".toCharArray(), salt)
        assertFalse(VaultCrypto.keysMatch(wrong, verifier))
    }

    @Test
    fun aesGcm_roundTrip() {
        val key = ByteArray(32) { 7 }
        val plain = "card-secret".toByteArray()
        val enc = VaultCrypto.encryptAesGcm(key, plain)
        val dec = VaultCrypto.decryptAesGcm(key, enc)
        assertArrayEquals(plain, dec)
    }
}
