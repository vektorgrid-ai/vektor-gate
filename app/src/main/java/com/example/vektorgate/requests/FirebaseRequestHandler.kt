package com.example.vektorgate.requests

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.vektorgate.MainActivity
import com.example.vektorgate.R
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.security.ToolInfo
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class FirebaseRequestHandler : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // For long-running tasks (10 seconds or more) use WorkManager
            // because Android might kill longer tasks to save battery
            if (needsToBeScheduled(remoteMessage.data)) {
                scheduleJob()
            } else {
                handleData(remoteMessage.data)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.body ?: "New Message")
        }
    }

    private fun needsToBeScheduled(dataPayload: Map<String, String>) = false // currently no tasks that are intended to be >10s

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun scheduleJob() {
        val work = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .build()
        WorkManager.getInstance(this)
            .beginWith(work)
            .enqueue()
    }

    private fun handleData(payload: Map<String, String>) {
        val manager = RequestManager(this)

        val request = ApprovalRequest(
            type = "approval_request",
            requestId = payload["request_id"] ?: "",
            nonce = payload["nonce"] ?: "",
            expiresAt = OffsetDateTime.parse(payload["expires_at"]).toLocalDateTime(),
            payloadHash = payload["payload_hash"] ?: "",
            tool = ToolInfo(
                name = payload["tool_name"] ?: "",
                description = payload["tool_description"] ?: "",
                riskLevel = payload["tool_risk_level"] ?: ""
            )
        )

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            manager.insertRequest(request)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
        
        // Save the token to DataStore using SettingsManager
        CoroutineScope(Dispatchers.IO).launch {
            SettingsManager.getInstance(applicationContext).saveFirebaseToken(token)
            
            // TODO: Implement the network call to your local server here
            // e.g. repository.updatePushToken(token)
        }
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+
        val channel = NotificationChannel(
            channelId,
            "Default Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Vektor Gate")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FirebaseRequestHandler"
    }

    internal class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            // This is only here so I don't have to look this up again when I actually need it
            return Result.success()
        }
    }
}