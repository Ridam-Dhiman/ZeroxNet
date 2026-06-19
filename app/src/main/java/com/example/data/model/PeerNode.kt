package com.example.data.model

data class PeerNode(
    val endpointId: String,
    val deviceId: String,
    val displayName: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val signalStrength: Int? = null
)
