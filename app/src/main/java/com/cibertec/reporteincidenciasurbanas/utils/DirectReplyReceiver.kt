package com.cibertec.reporteincidenciasurbanas.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DirectReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = results.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()?.trim()

        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            NotificationHelper.CHAT_NOTIFICATION_ID
        )

        if (!replyText.isNullOrBlank()) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val authRepository: AuthRepository = AuthRepositoryImpl()
                    val userRepository: UserRepository = UserRepositoryImpl()
                    val chatRepository: ChatRepository = ChatRepositoryImpl()

                    val currentUid = authRepository.getCurrentUserUid()
                        ?: FirebaseAuth.getInstance().currentUser?.uid

                    if (currentUid != null) {
                        val profileResource = userRepository.getUserProfile(currentUid).firstOrNull()
                        val user = if (profileResource is Resource.Success) profileResource.data else null

                        val authorName = user?.nombreCompleto?.ifBlank { user.correo }
                            ?: FirebaseAuth.getInstance().currentUser?.displayName
                            ?: "Usuario"

                        val authorRole = user?.rol ?: UserRole.CIUDADANO.name

                        val message = ChatMessage(
                            remitenteId = currentUid,
                            remitenteNombre = authorName,
                            remitenteRol = authorRole,
                            texto = replyText
                        )

                        chatRepository.sendMessage(message).firstOrNull()

                        chatRepository.markMessagesAsRead(currentUid)
                    }

                    NotificationHelper.updateNotificationAfterReply(context, notificationId, replyText)
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
