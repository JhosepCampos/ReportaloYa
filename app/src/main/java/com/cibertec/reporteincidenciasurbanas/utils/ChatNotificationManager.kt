package com.cibertec.reporteincidenciasurbanas.utils

import android.content.Context
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object ChatNotificationManager {

    var isChatScreenActive = false
        private set

    fun setChatScreenActive(active: Boolean) {
        isChatScreenActive = active
    }

    fun onFcmMessageReceived(
        context: Context,
        senderId: String,
        senderName: String,
        senderRole: String,
        messageText: String
    ) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (senderId.isNotBlank() && senderId == currentUid) return
        if (isChatScreenActive) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = firestore.collection(Constants.COLLECTION_CHATS)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val allMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }

                val unreadMessages = allMessages.filter { msg ->
                    msg.remitenteId != currentUid &&
                    !msg.leidoPor.contains(currentUid) &&
                    !msg.eliminado &&
                    msg.texto.isNotBlank()
                }

                if (unreadMessages.isNotEmpty()) {
                    NotificationHelper.showChatNotifications(
                        context = context,
                        unreadMessages = unreadMessages,
                        currentUserId = currentUid
                    )
                } else {
                    NotificationHelper.showChatNotification(
                        context = context,
                        senderId = senderId,
                        senderName = senderName,
                        senderRole = senderRole,
                        messageText = messageText
                    )
                }
            } catch (_: Exception) {
                NotificationHelper.showChatNotification(
                    context = context,
                    senderId = senderId,
                    senderName = senderName,
                    senderRole = senderRole,
                    messageText = messageText
                )
            }
        }
    }
}
