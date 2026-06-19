package com.example.nearby

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.MeshMessage
import com.example.data.model.PeerNode
import com.example.data.repository.MessageRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mesh_sos_channel"
        private const val CHANNEL_NAME = "Mesh SOS Network Service"

        private val _connectedPeers = MutableStateFlow<List<PeerNode>>(emptyList())
        val connectedPeers: StateFlow<List<PeerNode>> = _connectedPeers

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        var connectionManager: ConnectionManager? = null
        var meshRouter: MeshRouter? = null
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: MessageRepository

    override fun onCreate() {
        super.onCreate()
        Log.d("NearbyService", "Service onCreate")
        _isServiceRunning.value = true

        preferencesManager = PreferencesManager(this)
        val database = AppDatabase.getDatabase(this)
        repository = MessageRepository(database.messageDao())

        // Setup connection client and mesh router
        setupMeshComponents()

        // Setup channel and show Foreground notification
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getServiceNotification("Searching for mesh..."))

        // Setup lock management AFTER reaching foreground state to avoid AppOps start/stop mismatched states or background restrictions
        acquireLocks()

        // Observe identity changes to keep Nearby Connections up to date
        serviceScope.launch {
            preferencesManager.displayNameFlow.collect { name ->
                val deviceId = preferencesManager.getDeviceId()
                val finalName = name.ifEmpty { "Rescuer-${deviceId.takeLast(4)}" }
                connectionManager?.updateLocalIdentity(finalName, deviceId)
            }
        }

        // Start mesh loop
        startMesh()
    }

    private fun setupMeshComponents() {
        connectionManager = ConnectionManager(this) { message, senderId ->
            meshRouter?.processIncomingMessage(message, senderId)
        }

        meshRouter = MeshRouter(this, repository) {
            connectionManager
        }

        // Keep local connected peers synced with static flow for view consumption
        serviceScope.launch {
            connectionManager?.connectedPeers?.collect { peersMap ->
                val list = peersMap.values.toList()
                _connectedPeers.value = list

                // Update notification dynamically based on current peer count
                val description = if (list.isEmpty()) {
                    "Searching for mesh..."
                } else {
                    "● ${list.size} peer(s) in range"
                }
                updateNotificationDescription(description)
            }
        }
    }

    private fun startMesh() {
        Log.d("NearbyService", "Starting mesh networking...")
        connectionManager?.stopAll()
        connectionManager?.startAdvertising()
        connectionManager?.startDiscovery()
    }

    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZeroxNet::MeshWakeLock").apply {
            setReferenceCounted(false)
            acquire()
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZeroxNet::MeshWifiLock").apply {
            setReferenceCounted(false)
            acquire()
        }
        Log.d("NearbyService", "WakeLock and WifiLock acquired")
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.e("NearbyService", "Error releasing locks", e)
        }
        Log.d("NearbyService", "Locks released")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getServiceNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard Android system drawable for compatibility
        val iconRes = android.R.drawable.stat_notify_sync

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeshSOS Active — mesh running")
            .setContentText(contentText)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotificationDescription(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, getServiceNotification(text))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyService", "onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("NearbyService", "onTaskRemoved - restarting NearbyService")
        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            restartServicePendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("NearbyService", "Service onDestroy")
        _isServiceRunning.value = false
        connectionManager?.stopAll()
        releaseLocks()
        serviceJob.cancel()
    }
}
