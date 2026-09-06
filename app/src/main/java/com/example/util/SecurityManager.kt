package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "financeflow_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("financeflow_prefs_fallback", Context.MODE_PRIVATE)
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("biometric_enabled", value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var isDarkModeEnabled: Boolean
        get() = prefs.getBoolean("dark_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("dark_mode_enabled", value).apply()

    var isAutoTheme: Boolean
        get() = prefs.getBoolean("auto_theme", true)
        set(value) = prefs.edit().putBoolean("auto_theme", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    var selectedAccountId: String
        get() = prefs.getString("selected_account_id", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("selected_account_id", value).apply()

    fun storeEncryptedToken(key: String, token: String) {
        prefs.edit().putString("enc_token_$key", token).apply()
    }

    fun getEncryptedToken(key: String): String? {
        return prefs.getString("enc_token_$key", null)
    }
}
