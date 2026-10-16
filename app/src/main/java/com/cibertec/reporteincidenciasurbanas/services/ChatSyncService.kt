package com.cibertec.reporteincidenciasurbanas.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.cibertec.reporteincidenciasurbanas.MainActivity
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.utils.ChatNotificationManager
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatSyncService : Service() {

    companion object {
        private const val TAG = "ChatSyncService"
        const val CHANNEL_SYNC_ID = "channel_chat_background_sync"
        const val SYNC_NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, ChatSyncService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo iniciar ChatSyncService: ${e.message}")
            }
        }
    }

    private var firestoreListener: ListenerRegistration? = null
    private var isFirstLoad = true
    private val previousReactions = mutableMapOf<String, Map<String, String>>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createSyncNotificationChannel()
        startForegroundWithNotification()
        listenToFirestoreMessages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (firestoreListener == null) {
            listenToFirestoreMessages()
        }
        return START_STICKY
    }

    private fun createSyncNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_sync_name)
            val desc = getString(R.string.notification_channel_sync_desc)
            val channel = NotificationChannel(
                CHANNEL_SYNC_ID,
                name,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = desc
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SYNC_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_sync_title))
            .setContentText(getString(R.string.notification_sync_desc))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                SYNC_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            startForeground(SYNC_NOTIFICATION_ID, notification)
        }
    }

    private fun listenToFirestoreMessages() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val query = firestore.collection(Constants.COLLECTION_CHATS)
                .orderBy("timestamp", Query.Direction.ASCENDING)

            firestoreListener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error en Firestore sync: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val allMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }

                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                if (!isFirstLoad && currentUid.isNotBlank() && !ChatNotificationManager.isChatScreenActive) {
                    for (msg in allMessages) {
                        if (msg.remitenteId == currentUid && !msg.eliminado) {
                            val oldReactions = previousReactions[msg.id] ?: emptyMap()
                            for ((reactorId, emoji) in msg.reacciones) {
                                if (reactorId != currentUid && oldReactions[reactorId] != emoji) {
                                    val reactorName = getString(R.string.notification_reactor_fallback)
                                    val previewText = msg.texto.ifBlank { msg.reporteCodigo ?: "" }
                                    NotificationHelper.showReactionNotification(
                                        context = applicationContext,
                                        reactorName = reactorName,
                                        emoji = emoji,
                                        messageText = previewText
                                    )
                                }
                            }
                        }
                    }
                }

                for (msg in allMessages) {
                    previousReactions[msg.id] = msg.reacciones
                }

                if (isFirstLoad) {
                    isFirstLoad = false
                    return@addSnapshotListener
                }

                if (currentUid.isBlank()) return@addSnapshotListener

                if (ChatNotificationManager.isChatScreenActive) {
                    NotificationHelper.clearNotification(applicationContext)
                    return@addSnapshotListener
                }

                val unreadMessages = allMessages.filter { msg ->
                    msg.remitenteId != currentUid &&
                    !msg.leidoPor.contains(currentUid) &&
                    !msg.eliminado &&
                    (msg.texto.isNotBlank() || msg.esReporteCompartido)
                }

                if (unreadMessages.isNotEmpty()) {
                    NotificationHelper.showChatNotifications(
                        context = applicationContext,
                        unreadMessages = unreadMessages,
                        currentUserId = currentUid
                    )
                } else {
                    NotificationHelper.clearNotification(applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error iniciando listener de chat sync: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, ChatSyncService::class.java).also {
            it.setPackage(packageName)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartServiceIntent)
            } else {
                startService(restartServiceIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo reiniciar servicio en onTaskRemoved: ${e.message}")
        }
    }

    override fun onDestroy() {
        firestoreListener?.remove()
        firestoreListener = null
        super.onDestroy()
    }
}
