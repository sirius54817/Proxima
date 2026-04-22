package com.sirius.proxima.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityManager(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "proxima_security_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasPin(): Boolean {
        return !prefs.getString("pin_hash", null).isNullOrEmpty()
    }

    fun setupPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(pin.toByteArray(StandardCharsets.UTF_8))
        digest.update(salt)
        val hash = digest.digest()

        prefs.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltBase64 = prefs.getString("pin_salt", null) ?: return false
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val salt = Base64.decode(saltBase64, Base64.DEFAULT)

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(pin.toByteArray(StandardCharsets.UTF_8))
        digest.update(salt)
        val hash = Base64.encodeToString(digest.digest(), Base64.NO_WRAP)

        return hash == storedHash
    }

    fun changePin(newPin: String) {
        setupPin(newPin)
    }

    fun clearPin() {
        prefs.edit()
            .remove("pin_hash")
            .remove("pin_salt")
            .putBoolean("app_lock_enabled", false)
            .putBoolean("backup_lock_enabled", false)
            .putBoolean("biometric_enabled", false)
            .apply()
    }

    fun isAppLockEnabled(): Boolean {
        return prefs.getBoolean("app_lock_enabled", false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isBackupLockEnabled(): Boolean {
        return prefs.getBoolean("backup_lock_enabled", false)
    }

    fun setBackupLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("backup_lock_enabled", enabled).apply()
    }
}

