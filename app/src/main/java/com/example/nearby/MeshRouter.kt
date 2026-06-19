package com.example.nearby

import android.content.Context
import android.util.Log
import com.example.data.model.MeshMessage
import com.example.data.model.MessageType
import com.example.data.repository.MessageRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class MeshRouter(
    private val context: Context,
    private val repository: MessageRepository,
    private val broadcastProvider: () -> ConnectionManager?
) {
    // Message id to timestamp mapping
    private val seenMessageIds = ConcurrentHashMap<String, Long>()
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Simple continuous coroutine loop to clean seen ids older than 30 minutes
        scope.launch {
            while (true) {
                delay(60 * 1000) // check every minute
                val now = System.currentTimeMillis()
                val entryIterator = seenMessageIds.entries.iterator()
                while (entryIterator.hasNext()) {
                    val entry = entryIterator.next()
                    if (now - entry.value > 30 * 60 * 1000) { // 30 minutes threshold
                        entryIterator.remove()
                    }
                }
            }
        }
    }

    fun processIncomingMessage(message: MeshMessage, senderEndpointId: String) {
        val now = System.currentTimeMillis()
        val messageId = message.id

        if (seenMessageIds.containsKey(messageId)) {
            Log.d("MeshRouter", "Dropped duplicate message silently: $messageId")
            return
        }

        seenMessageIds[messageId] = now

        scope.launch {
            if (message.type == MessageType.ACK) {
                val originalMsgId = message.payload
                Log.d("MeshRouter", "Received ACK for original message ID: $originalMsgId")
                repository.updateDeliveryStatus(originalMsgId, "✓ Delivered")
                return@launch
            }

            // Save incoming message (it is not ours)
            val messageToSave = message.copy(deliveryStatus = "In Mesh")
            repository.insertMessage(messageToSave, isOwn = false)

            if (message.type == MessageType.SOS) {
                triggerSosAlert(message)
            }

            // Automatically send ACK for any regular / SOS messages back
            sendAckValue(message.id, senderEndpointId)

            // Decrement TTL by 1
            val updatedTtl = message.ttl - 1
            if (updatedTtl > 0) {
                val relayedMessage = message.copy(ttl = updatedTtl)
                Log.d("MeshRouter", "Relayed [${message.id}] from [${message.senderName}] - TTL remaining: $updatedTtl")
                broadcastProvider()?.broadcastMessage(relayedMessage)
            } else {
                Log.d("MeshRouter", "Message $messageId reached TTL 0. Relaying stopped.")
            }
        }
    }

    private fun sendAckValue(originalMessageId: String, senderEndpointId: String) {
        scope.launch {
            val ackMessage = MeshMessage(
                senderId = "ACK_NODE",
                senderName = "System",
                payload = originalMessageId,
                type = MessageType.ACK,
                ttl = 1
            )
            broadcastProvider()?.broadcastMessage(ackMessage)
            Log.d("MeshRouter", "Sent ACK back to mesh for message $originalMessageId")
        }
    }

    private fun triggerSosAlert(message: MeshMessage) {
        val intent = android.content.Intent("com.example.SOS_ALERT_RECEIVED").apply {
            putExtra("senderName", message.senderName)
            putExtra("payload", message.payload)
            putExtra("lat", message.lat ?: 0.0)
            putExtra("lon", message.lon ?: 0.0)
            putExtra("estimatedHop", 10 - message.ttl) // Max TTL for SOS is 10
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
        Log.d("MeshRouter", "Broadcast SOS Alert intent for ${message.id}")
    }
}
