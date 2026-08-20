package com.budgetnotes.app.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.core.content.edit
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Vault unlock: biometric / device-credential first (Keystore-wrapped random DB key).
 * App PIN is only used when the device has no strong biometric or screen lock.
 */
class VaultLockManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var sessionKey: ByteArray? = null

    val isSetup: Boolean
        get() = prefs.contains(KEY_VERIFIER) &&
            (prefs.contains(KEY_BIO_WRAPPED) || prefs.contains(KEY_SALT))

    val isUnlocked: Boolean
        get() = sessionKey != null

    val hasPinFallback: Boolean
        get() = prefs.contains(KEY_SALT)

    fun requireSessionKey(): ByteArray =
        sessionKey ?: error("Vault is locked")

    fun deviceSupportsSystemAuth(): Boolean {
        val bm = BiometricManager.from(appContext)
        return bm.canAuthenticate(SYSTEM_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prefersBiometricGate(): Boolean = deviceSupportsSystemAuth()

    fun lock() {
        VaultCrypto.clear(sessionKey)
        sessionKey = null
    }

    // --- Biometric / device-credential path ---

    /** Cipher for first-time vault creation (encrypt a new random DB key). */
    fun createBiometricCipherForEncrypt(): Cipher {
        deleteBiometricKeyIfPresent()
        val secretKey = createBiometricKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    /** Cipher for unlocking an existing biometric-wrapped vault. */
    fun createBiometricCipherForDecrypt(): Cipher? {
        if (!prefs.contains(KEY_BIO_WRAPPED) || !prefs.contains(KEY_BIO_IV)) return null
        return try {
            val secretKey = getExistingBiometricKey() ?: return null
            val iv = Base64.decode(prefs.getString(KEY_BIO_IV, null), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            cipher
        } catch (_: Exception) {
            null
        }
    }

    /**
     * After BiometricPrompt succeeds with an encrypt CryptoObject on first launch.
     */
    fun setupWithBiometricCipher(cipher: Cipher): Boolean {
        return try {
            val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val encrypted = cipher.doFinal(key)
            val iv = cipher.iv
            prefs.edit {
                putString(KEY_BIO_WRAPPED, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                putString(KEY_BIO_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                putString(KEY_VERIFIER, VaultCrypto.toBase64(VaultCrypto.keyVerifier(key)))
                // Biometric-primary vaults do not require an app PIN.
                remove(KEY_SALT)
            }
            sessionKey = key
            true
        } catch (_: Exception) {
            false
        }
    }

    fun unlockWithBiometricCipher(cipher: Cipher): Boolean {
        val wrappedB64 = prefs.getString(KEY_BIO_WRAPPED, null) ?: return false
        val verifierB64 = prefs.getString(KEY_VERIFIER, null) ?: return false
        return try {
            val wrapped = Base64.decode(wrappedB64, Base64.NO_WRAP)
            val key = cipher.doFinal(wrapped)
            val verifier = VaultCrypto.fromBase64(verifierB64)
            if (!VaultCrypto.keysMatch(key, verifier)) {
                VaultCrypto.clear(key)
                return false
            }
            sessionKey = key
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- App PIN path (devices without biometric / screen lock) ---

    fun setupPin(pin: CharArray): ByteArray {
        require(pin.size == 6 && pin.all { it.isDigit() }) { "PIN must be 6 digits" }
        val salt = VaultCrypto.generateSalt()
        val key = VaultCrypto.deriveKey(pin, salt)
        prefs.edit {
            putString(KEY_SALT, VaultCrypto.toBase64(salt))
            putString(KEY_VERIFIER, VaultCrypto.toBase64(VaultCrypto.keyVerifier(key)))
            remove(KEY_BIO_WRAPPED)
            remove(KEY_BIO_IV)
        }
        deleteBiometricKeyIfPresent()
        sessionKey = key.copyOf()
        return key
    }

    fun unlockWithPin(pin: CharArray): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val verifierB64 = prefs.getString(KEY_VERIFIER, null) ?: return false
        val salt = VaultCrypto.fromBase64(saltB64)
        val verifier = VaultCrypto.fromBase64(verifierB64)
        val key = VaultCrypto.deriveKey(pin, salt)
        return if (VaultCrypto.keysMatch(key, verifier)) {
            sessionKey = key
            true
        } else {
            VaultCrypto.clear(key)
            false
        }
    }

    private fun getExistingBiometricKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(BIO_KEY_ALIAS, null) as? SecretKey
    }

    private fun deleteBiometricKeyIfPresent() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(BIO_KEY_ALIAS)) {
                keyStore.deleteEntry(BIO_KEY_ALIAS)
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun createBiometricKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            BIO_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(0)
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    companion object {
        const val SYSTEM_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        private const val PREFS_NAME = "vault_lock"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_BIO_WRAPPED = "bio_wrapped"
        private const val KEY_BIO_IV = "bio_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIO_KEY_ALIAS = "budget_notes_vault_bio"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
