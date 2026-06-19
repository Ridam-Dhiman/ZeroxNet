package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val payload: String,
    val timestamp: Long,
    val type: String, // NORMAL, SOS, ACK, PING
    val lat: Double?,
    val lon: Double?,
    val isOwn: Boolean,
    var deliveryStatus: String
)
