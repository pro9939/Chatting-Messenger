package com.chatting.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.chatting.domain.AccountManager
import com.chatting.domain.MediaManager
import com.chatting.domain.MessageManager
import com.chatting.ui.helper.ExceptionHandler
import com.data.repository.*
import com.chatting.ui.theme.ThemeManager
import com.chatting.ui.utils.SecurePrefs
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.chatting.vibration.VibrationManager
import com.chatting.websock.WebSocketClient
import com.chatting.websock.WebSocketMessageHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var messageManager: MessageManager

    @Inject
    lateinit var mediaManager: MediaManager

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var webSocketClient: WebSocketClient

    @Inject
    lateinit var webSocketMessageHandler: WebSocketMessageHandler

    companion object {
        private const val TAG = "MyApp"
        fun getRepository(context: Context): ChatRepository {
            return (context.applicationContext as MyApp).chatRepository
        }
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            if (authRepository.authStateFlow.value == AuthState.LOGGED_IN) {
                Log.d(TAG, "App in foreground AND Logged In. Attempting WebSocket connection...")
                try {
                    webSocketClient.connect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting WebSocket onStart", e)
                }
            } else {
                Log.d(TAG, "App in foreground but Logged Out. Skipping WebSocket connection.")
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            Log.d(TAG, "App in background. Disconnecting WebSocket.")
            try {
                webSocketClient.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting WebSocket onStop", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate started.")

        SecurePrefs.init(this)
        Log.d(TAG, "SecurePrefs initialized.")

        VibrationManager.init(this)

        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this))
        Log.d(TAG, "ExceptionHandler set.")

        FirebaseApp.initializeApp(this)
        Log.d(TAG, "FirebaseApp initialized.")

        ThemeManager.init(this)
        Log.d(TAG, "ThemeManager initialized.")

        webSocketMessageHandler.startListening()
        Log.d(TAG, "WebSocketMessageHandler listener started.")

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        Log.d(TAG, "LifecycleObserver added.")

        getAndStoreFCMToken()
        Log.d(TAG, "onCreate finished.")
    }

    private fun getAndStoreFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null) {
                    SecurePrefs.putString("FMC", token)
                    Log.d(TAG, "FCM Token obtained and saved: ${token.take(10)}...")
                } else {
                    Log.w(TAG, "FCM Token obtained but was null.")
                }
            } else {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
        }
    }
}