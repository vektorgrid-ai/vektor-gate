package com.example.vektorgate.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager private constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val CORE_URL_KEY = stringPreferencesKey("core_url")
        private val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
        private val FIREBASE_TOKEN_KEY = stringPreferencesKey("firebase_token")

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
}
