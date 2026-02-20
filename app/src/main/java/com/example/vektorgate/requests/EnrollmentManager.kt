package com.example.vektorgate.requests

import android.util.Log
import com.example.vektorgate.data.ConnectionStatus
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EnrollmentManager(
    private val securityManager: SecurityManager,
    private val settingsManager: SettingsManager,
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun enroll(serverUrl: String, deviceName: String, firebaseToken: String) {
        var deviceId = settingsManager.deviceId.first()
        if (deviceId == null) {
            deviceId = Uuid.random().toHexDashString()
            settingsManager.saveDeviceId(deviceId)
        }

        if (serverUrl.isEmpty()) {
            settingsManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        settingsManager.setConnectionStatus(ConnectionStatus.CONNECTING)

        if (!securityManager.hasKey()) securityManager.generateKeyPair()
        val publicKey = securityManager.getPublicKeyBase64() ?: return

        try {
            val enrollmentSuccess = sendEnrollMessage(serverUrl, publicKey, deviceName, firebaseToken, deviceId)
            if (enrollmentSuccess) {
                settingsManager.setConnectionStatus(ConnectionStatus.CONNECTED)
            } else {
                settingsManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            }
        } catch (e: Exception) {
            Log.e("EnrollmentManager", "Enrollment failed", e)
            settingsManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        }
    }

    private suspend fun sendEnrollMessage(
        serverUrl: String,
        publicKey: String,
        deviceName: String,
        firebaseToken: String,
        deviceId: String): Boolean
    = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val enrollRequest = EnrollmentRequest(
            device_name = deviceName,
            public_key = publicKey,
            firebase_token = firebaseToken,
            device_id = deviceId
        )
        val body = Json.encodeToString(enrollRequest).toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$serverUrl/companion/enroll")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("EnrollmentManager", "Failed to enroll: ${response.code}")
                    return@withContext false
                }
                true
            }
        } catch (e: IOException) {
            Log.e("EnrollmentManager", "Network error during enrollment", e)
            false
        }
    }
}
