package com.cibertec.reporteincidenciasurbanas.utils

import android.util.Log
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FcmNotificationSender {

    private const val TAG = "FcmNotificationSender"
    private const val FCM_TOPIC = "/topics/chat_comunidad"

    private const val LEGACY_SERVER_KEY = "AAAA-DEFAULT-KEY"

    suspend fun sendPushNotification(message: ChatMessage) = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://fcm.googleapis.com/fcm/send")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Authorization", "key=$LEGACY_SERVER_KEY")
            connection.doOutput = true
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            val jsonBody = JSONObject().apply {
                put("to", FCM_TOPIC)
                put("priority", "high")
                put("content_available", true)

                val dataObj = JSONObject().apply {
                    put("senderId", message.remitenteId)
                    put("senderName", message.remitenteNombre)
                    put("senderRole", message.remitenteRol)
                    put("message", message.texto)
                    put("timestamp", message.timestamp?.toDate()?.time ?: System.currentTimeMillis())
                }
                put("data", dataObj)

                val notificationObj = JSONObject().apply {
                    put("title", message.remitenteNombre)
                    put("body", message.texto)
                    put("sound", "default")
                    put("android_channel_id", NotificationHelper.CHANNEL_ID_CHAT)
                }
                put("notification", notificationObj)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Envío FCM a $FCM_TOPIC completado con código HTTP: $responseCode")
        } catch (e: Exception) {
            Log.w(TAG, "Aviso: No se pudo enviar notificación FCM directa (se gestiona vía Cloud Function o listener local): ${e.message}")
        }
    }
}
