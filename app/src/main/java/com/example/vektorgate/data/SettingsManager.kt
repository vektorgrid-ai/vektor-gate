package com.example.vektorgate.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}

class SettingsManager private constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val CORE_URL_KEY = stringPreferencesKey("core_url")
        private val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        private val FIREBASE_TOKEN_KEY = stringPreferencesKey("firebase_token")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext.dataStore)
                INSTANCE = instance
                instance
            }
        }
    }

    val coreUrl: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[CORE_URL_KEY] ?: ""
        }

    val deviceName: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_NAME_KEY] ?: android.os.Build.MODEL
        }

    val firebaseToken: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[FIREBASE_TOKEN_KEY] ?: "Not Set"
        }

    val deviceId: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_ID_KEY]
        }

    // Store connection status in-memory
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    suspend fun saveCoreUrl(url: String) {
        Log.d("SettingsManager", "Saving core URL: $url")
        dataStore.edit { preferences ->
            preferences[CORE_URL_KEY] = url
        }
    }

    suspend fun saveDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_NAME_KEY] = name
        }
    }

    suspend fun saveFirebaseToken(token: String) {
        dataStore.edit { preferences ->
            preferences[FIREBASE_TOKEN_KEY] = token
        }
    }

    suspend fun saveDeviceId(id: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = id
        }
    }

    fun setConnectionStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }
}
