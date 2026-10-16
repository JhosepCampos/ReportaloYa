package com.cibertec.reporteincidenciasurbanas.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class IncidenciasMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuevo token generado: $token")

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUid)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w("FCM", "Error al actualizar token FCM: ${e.message}")
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val data = remoteMessage.data

        val senderId = data["senderId"] ?: ""
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "Mensaje del Chat"
        val senderRole = data["senderRole"] ?: "CIUDADANO"
        val messageText = data["message"] ?: remoteMessage.notification?.body ?: ""

        if (senderId.isNotBlank() && senderId == currentUid) {
            return
        }

        if (messageText.isNotBlank()) {
            ChatNotificationManager.onFcmMessageReceived(
                context = applicationContext,
                senderId = senderId,
                senderName = senderName,
                senderRole = senderRole,
                messageText = messageText
            )
        }
    }
}
