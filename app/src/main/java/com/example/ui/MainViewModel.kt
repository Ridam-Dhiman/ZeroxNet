package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.MeshMessage
import com.example.data.model.MessageType
import com.example.data.model.PeerNode
import com.example.data.repository.MessageRepository
import com.example.nearby.NearbyService
import com.example.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository: MessageRepository
    private val preferencesManager: PreferencesManager
    private val locationHelper: LocationHelper

    // UI state flows
    val messages: StateFlow<List<MeshMessage>>
    val connectedPeers: StateFlow<List<PeerNode>> = NearbyService.connectedPeers
    val isServiceRunning: StateFlow<Boolean> = NearbyService.isServiceRunning

    // Preferences states
    val deviceId = MutableStateFlow("")
    val displayName = MutableStateFlow("")
    val autoShareLocation = MutableStateFlow(true)
    val playSoundOnSos = MutableStateFlow(true)
    val vibrateOnIncoming = MutableStateFlow(true)

    // SOS alert state for overlay
    data class SosAlertState(
        val isTriggered: Boolean = false,
        val senderName: String = "",
        val payload: String = "",
        val lat: Double = 0.0,
        val lon: Double = 0.0,
        val estimatedHop: Int = 0
    )
    private val _incomingSosAlert = MutableStateFlow(SosAlertState())
    val incomingSosAlert: StateFlow<SosAlertState> = _incomingSosAlert

    private var activeRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val sosReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.SOS_ALERT_RECEIVED") {
                val sender = intent.getStringExtra("senderName") ?: "Unknown"
                val payload = intent.getStringExtra("payload") ?: "SOS Alert"
                val lat = intent.getDoubleExtra("lat", 0.0)
                val lon = intent.getDoubleExtra("lon", 0.0)
                val hop = intent.getIntExtra("estimatedHop", 0)

                _incomingSosAlert.value = SosAlertState(
                    isTriggered = true,
                    senderName = sender,
                    payload = payload,
                    lat = lat,
                    lon = lon,
                    estimatedHop = hop
                )

                // Trigger alerts based on user settings
                triggerIncomingAlerts()
            }
        }
    }

    init {
        val database = AppDatabase.getDatabase(context)
        repository = MessageRepository(database.messageDao())
        preferencesManager = PreferencesManager(context)
        locationHelper = LocationHelper(context)

        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read preferences
        viewModelScope.launch {
            deviceId.value = preferencesManager.getDeviceId()
            preferencesManager.displayNameFlow.collect { displayName.value = it }
        }
        viewModelScope.launch {
            preferencesManager.autoShareLocationFlow.collect { autoShareLocation.value = it }
        }
        viewModelScope.launch {
            preferencesManager.playSoundOnSosFlow.collect { playSoundOnSos.value = it }
        }
        viewModelScope.launch {
            preferencesManager.vibrateOnIncomingFlow.collect { vibrateOnIncoming.value = it }
        }

        // Register for incoming SOS alert broadcast from MeshRouter
        val filter = IntentFilter("com.example.SOS_ALERT_RECEIVED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(sosReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(sosReceiver, filter)
        }

        // Run auto cleanup: Auto-delete messages older than 72 hours
        viewModelScope.launch(Dispatchers.IO) {
            val cutOff = System.currentTimeMillis() - (72 * 60 * 60 * 1000)
            repository.deleteOlderThan(cutOff)
            Log.d("MainViewModel", "Auto-cleaned messages older than 72 hours")
        }
    }

    private fun triggerIncomingAlerts() {
        // Sound Alert
        if (playSoundOnSos.value) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                activeRingtone = RingtoneManager.getRingtone(context, notificationUri)
                activeRingtone?.play()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error playing SOS sound alert", e)
            }
        }

        // Vibration alert: pattern [0, 500, 200, 500, 200, 500]
        if (vibrateOnIncoming.value) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        }

        // Show persistent high-priority notification channel
        showPrioritySosNotification()
    }

    private fun showPrioritySosNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "sos_priority_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Mesh Priority SOS Alert",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("EMERGENCY SOS RECEIVED")
            .setContentText("${_incomingSosAlert.value.senderName}: ${_incomingSosAlert.value.payload}")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }

    fun dismissSosAlert() {
        _incomingSosAlert.value = SosAlertState(isTriggered = false)
        activeRingtone?.stop()
        vibrator?.cancel()
    }

    fun saveOnboarding(name: String) {
        viewModelScope.launch {
            preferencesManager.setDisplayName(name)
            // Starts the background Foreground service immediately to run mesh
            startService()
        }
    }

    fun startService() {
        val serviceIntent = Intent(context, NearbyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopService() {
        val serviceIntent = Intent(context, NearbyService::class.java)
        context.stopService(serviceIntent)
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(text: String, isSos: Boolean = false) {
        if (text.trim().isEmpty() && !isSos) return

        viewModelScope.launch {
            val dName = displayName.value.ifEmpty { "Rescuer-${deviceId.value.takeLast(4)}" }
            var lat: Double? = null
            var lon: Double? = null

            // If location allowed, append GPS location tagging
            if (autoShareLocation.value) {
                val loc = locationHelper.getLastLocation()
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                }
            }

            val message = MeshMessage(
                senderId = deviceId.value,
                senderName = dName,
                payload = if (isSos) (if (text.trim().isEmpty()) "SOS EMERGENCY CALL!" else text) else text,
                ttl = if (isSos) 10 else 7,
                type = if (isSos) MessageType.SOS else MessageType.NORMAL,
                lat = lat,
                lon = lon,
                deliveryStatus = "Sending"
            )

            // 1. Insert to own local Room database
            repository.insertMessage(message, isOwn = true)

            // 2. Broadcast via service client
            val manager = NearbyService.connectionManager
            if (manager != null) {
                manager.broadcastMessage(message)
                // Mark immediately as "In Mesh" since we sent it out
                repository.updateDeliveryStatus(message.id, "In Mesh")
            } else {
                Log.w("MainViewModel", "Service manager offline. Kept saved locally.")
            }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            preferencesManager.setDisplayName(name)
        }
    }

    fun updateAutoShareLocation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoShareLocation(enabled)
        }
    }

    fun updatePlaySoundOnSos(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPlaySoundOnSos(enabled)
        }
    }

    fun updateVibrateOnIncoming(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setVibrateOnIncoming(enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOlderThan(System.currentTimeMillis() + 100000)
            Log.d("MainViewModel", "Database cleared successfully")
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(sosReceiver)
        } catch (e: Exception) {
            // ignore
        }
        activeRingtone?.stop()
        vibrator?.cancel()
    }
}
