package com.chatting.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chatting.ui.MainActivity
import com.chatting.ui.R
import com.chatting.websock.WebSocketClient
import com.chatting.websock.WebSocketState
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var webSocketClient: WebSocketClient

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        val currentWebSocketState = runBlocking { webSocketClient.connectionState.first() }

        if (currentWebSocketState == WebSocketState.CONNECTED) {
            Log.d(TAG, "App is likely in foreground (WebSocket connected), notification skipped.")
            return
        }

        var notificationTitle = getString(R.string.app_name)
        var notificationBody = "Você tem uma nova mensagem."

        remoteMessage.notification?.let {
            notificationTitle = it.title ?: notificationTitle
            notificationBody = it.body ?: notificationBody
            Log.d(TAG, "Received Notification Payload - Title: ${it.title}, Body: ${it.body}")
        }

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Received Data Payload: ${remoteMessage.data}")
            notificationTitle = remoteMessage.data["title"] ?: notificationTitle
            notificationBody = remoteMessage.data["body"] ?: notificationBody
        }

        Log.d(TAG, "Showing Notification - Title: $notificationTitle, Body: $notificationBody")
        sendNotification(notificationTitle, notificationBody)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        Log.d(TAG, "Sending FCM token to server (implementation needed): $token")
    }

    private fun sendNotification(messageTitle: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.app_name)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = getString(R.string.app_name)
            val channelDescription = "Notificações de novas mensagens"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
