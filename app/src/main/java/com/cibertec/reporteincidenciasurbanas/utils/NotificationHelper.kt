package com.cibertec.reporteincidenciasurbanas.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.cibertec.reporteincidenciasurbanas.MainActivity
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth

object NotificationHelper {

    const val CHANNEL_ID_CHAT = "channel_community_chat"
    const val KEY_TEXT_REPLY = "key_chat_direct_reply"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"
    const val CHAT_NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_chat_name)
            val descriptionText = context.getString(R.string.notification_channel_chat_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_CHAT, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showChatNotifications(
        context: Context,
        notificationId: Int = CHAT_NOTIFICATION_ID,
        unreadMessages: List<ChatMessage>,
        currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    ) {
        if (unreadMessages.isEmpty()) {
            clearNotification(context, notificationId)
            return
        }

        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
        }
        val tapPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyLabel = context.getString(R.string.notification_reply_placeholder)
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val lastSenderName = unreadMessages.lastOrNull()?.remitenteNombre ?: "Chat Comunitario"
        val replyIntent = Intent(context, DirectReplyReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_SENDER_NAME, lastSenderName)
        }
        val replyPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            context.getString(R.string.notification_action_reply),
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val userPerson = Person.Builder()
            .setName("Tú")
            .setKey(currentUserId)
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(context.getString(R.string.chat_title))
            .setGroupConversation(true)

        for (msg in unreadMessages) {
            val roleBadge = if (msg.remitenteRol.equals("ADMINISTRADOR", ignoreCase = true)) " [ADMIN]" else ""
            val senderDisplayName = "${msg.remitenteNombre}$roleBadge"

            val senderPerson = Person.Builder()
                .setName(senderDisplayName)
                .setKey(msg.remitenteId)
                .build()

            val msgTime = msg.timestamp?.toDate()?.time ?: System.currentTimeMillis()
            messagingStyle.addMessage(
                msg.texto,
                msgTime,
                senderPerson
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showChatNotification(
        context: Context,
        notificationId: Int = CHAT_NOTIFICATION_ID,
        senderId: String,
        senderName: String,
        senderRole: String,
        messageText: String
    ) {
        val singleMessage = ChatMessage(
            remitenteId = senderId,
            remitenteNombre = senderName,
            remitenteRol = senderRole,
            texto = messageText
        )
        showChatNotifications(
            context = context,
            notificationId = notificationId,
            unreadMessages = listOf(singleMessage)
        )
    }

    fun updateNotificationAfterReply(
        context: Context,
        notificationId: Int,
        replyText: String
    ) {
        val confirmationText = context.getString(R.string.notification_reply_sent_prefix, replyText)

        val repliedNotification = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.chat_title))
            .setContentText(confirmationText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setTimeoutAfter(3000)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, repliedNotification)
        } catch (_: SecurityException) {
        }
    }

    const val REACTION_NOTIFICATION_ID = 1002

    fun showReactionNotification(
        context: Context,
        reactorName: String,
        emoji: String,
        messageText: String
    ) {
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
        }
        val tapPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            REACTION_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_reaction_title)
        val body = if (messageText.isNotBlank()) {
            context.getString(R.string.notification_reaction_body, reactorName, emoji, messageText)
        } else {
            context.getString(R.string.notification_reaction_body_short, reactorName, emoji)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(REACTION_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun clearNotification(context: Context, notificationId: Int = CHAT_NOTIFICATION_ID) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        } catch (_: Exception) {
        }
    }
}
