package com.example.data.model

import java.util.UUID

data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    var ttl: Int = 7,
    val type: MessageType = MessageType.NORMAL,
    val lat: Double? = null,
    val lon: Double? = null,
    var deliveryStatus: String = "Sending"
)
