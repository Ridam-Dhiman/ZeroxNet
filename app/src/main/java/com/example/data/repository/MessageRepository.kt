package com.example.data.repository

import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.model.MeshMessage
import com.example.data.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(private val messageDao: MessageDao) {

    val allMessages: Flow<List<MeshMessage>> = messageDao.getAllMessages().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun insertMessage(message: MeshMessage, isOwn: Boolean) {
        messageDao.insertMessage(message.toEntity(isOwn))
    }

    suspend fun getMessageById(id: String): MeshMessage? {
        return messageDao.getMessageById(id)?.toDomainModel()
    }

    suspend fun deleteOlderThan(timestamp: Long) {
        messageDao.deleteOlderThan(timestamp)
    }

    suspend fun updateDeliveryStatus(id: String, status: String) {
        messageDao.updateDeliveryStatus(id, status)
    }

    private fun MessageEntity.toDomainModel(): MeshMessage {
        return MeshMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            payload = payload,
            timestamp = timestamp,
            type = try { MessageType.valueOf(type) } catch (e: Exception) { MessageType.NORMAL },
            lat = lat,
            lon = lon,
            deliveryStatus = deliveryStatus
        )
    }

    private fun MeshMessage.toEntity(isOwn: Boolean): MessageEntity {
        return MessageEntity(
            id = id,
            senderId = senderId,
            senderName = senderName,
            payload = payload,
            timestamp = timestamp,
            type = type.name,
            lat = lat,
            lon = lon,
            isOwn = isOwn,
            deliveryStatus = deliveryStatus
        )
    }
}
