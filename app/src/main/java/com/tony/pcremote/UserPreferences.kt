package com.tony.pcremote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(val context: Context) {

    companion object {
        val KEY_USERNAME   = stringPreferencesKey("username")
        val KEY_PASS_HASH  = stringPreferencesKey("password_hash")
        val KEY_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_LICENSE    = stringPreferencesKey("license_key")
        val KEY_STREAM_W   = intPreferencesKey("stream_width")
        val KEY_STREAM_H   = intPreferencesKey("stream_height")
        val KEY_STREAM_FPS = intPreferencesKey("stream_fps")
        val KEY_STREAM_Q   = intPreferencesKey("stream_quality")

        // Local sha256 — no longer depends on LicenseManager
        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val username:   Flow<String>  = context.dataStore.data.map { it[KEY_USERNAME]   ?: "" }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOGGED_IN]  ?: false }
    val isPremium:  Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_PREMIUM] ?: false }
    val licenseKey: Flow<String>  = context.dataStore.data.map { it[KEY_LICENSE]    ?: "" }

    val streamConfig: Flow<StreamConfig> = context.dataStore.data.map {
        StreamConfig(
            width   = it[KEY_STREAM_W]   ?: 960,
            height  = it[KEY_STREAM_H]   ?: 540,
            fps     = it[KEY_STREAM_FPS] ?: 30,
            quality = it[KEY_STREAM_Q]   ?: 45
        )
    }

    // 🔹 LOCAL REGISTER (offline)
    suspend fun register(username: String, password: String): Boolean {
        if (username.isBlank() || password.length < 4) return false
        context.dataStore.edit {
            it[KEY_USERNAME]  = username.trim().lowercase()
            it[KEY_PASS_HASH] = sha256(password)
            it[KEY_LOGGED_IN] = true
        }
        return true
    }

    // 🔹 LOCAL LOGIN (offline)
    suspend fun login(username: String, password: String): Boolean {
        val prefs      = context.dataStore.data.first()
        val storedUser = prefs[KEY_USERNAME]  ?: ""
        val storedHash = prefs[KEY_PASS_HASH] ?: ""

        return if (storedUser == username.trim().lowercase() &&
            storedHash == sha256(password)) {
            context.dataStore.edit { it[KEY_LOGGED_IN] = true }
            true
        } else false
    }

    // 🔥 NEW — SERVER AUTH (use after API success)
    suspend fun saveUserSession(
        username: String,
        password: String,
        isPremium: Boolean = false
    ) {
        context.dataStore.edit {
            it[KEY_USERNAME]  = username.trim().lowercase()
            it[KEY_PASS_HASH] = sha256(password)
            it[KEY_LOGGED_IN] = true
            if (isPremium) it[KEY_IS_PREMIUM] = true
        }
    }

    // Called after API confirms license is valid
    suspend fun activateLicenseLocal(key: String) {
        context.dataStore.edit {
            it[KEY_IS_PREMIUM] = true
            it[KEY_LICENSE]    = key.trim().uppercase()
        }
    }

    suspend fun saveStreamConfig(config: StreamConfig) {
        context.dataStore.edit {
            it[KEY_STREAM_W]   = config.width
            it[KEY_STREAM_H]   = config.height
            it[KEY_STREAM_FPS] = config.fps
            it[KEY_STREAM_Q]   = config.quality
        }
    }

    suspend fun logout() {
        context.dataStore.edit {
            it[KEY_LOGGED_IN] = false
        }
    }
}