package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    fun sendMessage(message: ChatMessage): Flow<Resource<Unit>>
    fun editMessage(messageId: String, newText: String, userId: String): Flow<Resource<Unit>>
    fun deleteMessage(messageId: String, userId: String): Flow<Resource<Unit>>
    fun toggleReaction(messageId: String, emoji: String, userId: String): Flow<Resource<Unit>>
    suspend fun markMessagesAsRead(userId: String)
}
