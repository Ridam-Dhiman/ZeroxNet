package com.example.nearby

import android.content.Context
import android.util.Log
import com.example.data.model.PeerNode
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.gson.Gson
import com.example.data.model.MeshMessage
import java.nio.charset.StandardCharsets

class ConnectionManager(
    private val context: Context,
    private val serviceId: String = "com.meshsos.nearby",
    private val strategy: Strategy = Strategy.P2P_CLUSTER,
    private val onMessageReceived: (MeshMessage, String) -> Unit // message, senderEndpointId
) {
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()

    // Map of connected endpoints: Map<endpointId, PeerNode>
    private val _connectedPeers = MutableStateFlow<Map<String, PeerNode>>(emptyMap())
    val connectedPeers: StateFlow<Map<String, PeerNode>> = _connectedPeers

    // Local device display name and device ID (assigned when service starts or changes)
    private var localName: String = "Anonymous"
    private var localDeviceId: String = ""

    fun updateLocalIdentity(name: String, deviceId: String) {
        localName = name
        localDeviceId = deviceId
    }

    // Callbacks for connection requests
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val json = String(bytes, StandardCharsets.UTF_8)
                try {
                    val message = gson.fromJson(json, MeshMessage::class.java)
                    if (message != null) {
                        Log.d("ConnectionManager", "Received message: ${message.id} from endpoint: $endpointId")
                        onMessageReceived(message, endpointId)
                    }
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Error parsing message payload", e)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // No-op or minor logs
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("ConnectionManager", "Connection initiated with $endpointId (${info.endpointName})")
            // Auto-accept all connections for mesh emergency use
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.d("ConnectionManager", "Auto-accepted connection with $endpointId")
                }
                .addOnFailureListener { e ->
                    Log.e("ConnectionManager", "Failed to accept connection with $endpointId", e)
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d("ConnectionManager", "Connected successfully to $endpointId")
                    val peerName = "Peer-${endpointId.takeLast(4)}"
                    val peer = PeerNode(
                        endpointId = endpointId,
                        deviceId = endpointId, // Can fallback to endpointId or update once verified
                        displayName = peerName,
                        lastSeen = System.currentTimeMillis()
                    )
                    _connectedPeers.value = _connectedPeers.value + (endpointId to peer)
                    Log.d("ConnectionManager", "Current connected count: ${_connectedPeers.value.size}")
                }
                else -> {
                    Log.w("ConnectionManager", "Connection failed to $endpointId with status ${result.status}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("ConnectionManager", "Disconnected from $endpointId")
            _connectedPeers.value = _connectedPeers.value - endpointId
            // Callback or notification trigger for UI is automated by Flow
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d("ConnectionManager", "Endpoint found: $endpointId (${info.endpointName}) on serviceId: ${info.serviceId}")
            if (info.serviceId == serviceId) {
                Log.d("ConnectionManager", "Requesting connection to $endpointId")
                connectionsClient.requestConnection(localName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d("ConnectionManager", "Requested connection to $endpointId successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ConnectionManager", "Failed to request connection to $endpointId", e)
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d("ConnectionManager", "Endpoint lost: $endpointId")
            _connectedPeers.value = _connectedPeers.value - endpointId
        }
    }

    fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startAdvertising(
            localName,
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d("ConnectionManager", "Advertising started successfully")
        }.addOnFailureListener { e ->
            Log.e("ConnectionManager", "Advertising failed to start", e)
        }
    }

    fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startDiscovery(
            serviceId,
            discoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.d("ConnectionManager", "Discovery started successfully")
        }.addOnFailureListener { e ->
            Log.e("ConnectionManager", "Discovery failed to start", e)
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        Log.d("ConnectionManager", "Advertising stopped")
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        Log.d("ConnectionManager", "Discovery stopped")
    }

    fun stopAll() {
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        _connectedPeers.value = emptyMap()
        Log.d("ConnectionManager", "Mesh connected clients reset")
    }

    fun broadcastMessage(message: MeshMessage) {
        val json = gson.toJson(message)
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val payload = Payload.fromBytes(bytes)
        val endpoints = _connectedPeers.value.keys

        if (endpoints.isNotEmpty()) {
            connectionsClient.sendPayload(endpoints.toList(), payload)
                .addOnSuccessListener {
                    Log.d("ConnectionManager", "Broadcasted payload ${message.id} to endpoints: $endpoints")
                }
                .addOnFailureListener { e ->
                    Log.e("ConnectionManager", "Failed to broadcast payload ${message.id}", e)
                }
        } else {
            Log.w("ConnectionManager", "No endpoints currently connected; caching payload locally")
        }
    }

    fun sendPayloadTo(endpointId: String, payload: Payload) {
        connectionsClient.sendPayload(endpointId, payload)
    }
}
