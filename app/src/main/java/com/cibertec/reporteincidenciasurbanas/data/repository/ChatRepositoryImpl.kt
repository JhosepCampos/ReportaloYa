package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val query = firestore.collection(Constants.COLLECTION_CHATS)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                }
                trySend(messages)
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }.flowOn(Dispatchers.IO)

    override fun sendMessage(message: ChatMessage): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val collection = firestore.collection(Constants.COLLECTION_CHATS)
            val docRef = collection.document()
            val now = Timestamp.now()

            val newMsg = message.copy(
                id = docRef.id,
                timestamp = now,
                editado = false,
                eliminado = false,
                reacciones = emptyMap(),
                leidoPor = listOf(message.remitenteId)
            )

            docRef.set(newMsg).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al enviar mensaje: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun editMessage(
        messageId: String,
        newText: String,
        userId: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val docRef = firestore.collection(Constants.COLLECTION_CHATS).document(messageId)
            docRef.update(
                mapOf(
                    "texto" to newText.trim(),
                    "editado" to true
                )
            ).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al editar el mensaje: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteMessage(
        messageId: String,
        userId: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val docRef = firestore.collection(Constants.COLLECTION_CHATS).document(messageId)
            docRef.update(
                mapOf(
                    "eliminado" to true,
                    "texto" to ""
                )
            ).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar el mensaje: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun toggleReaction(
        messageId: String,
        emoji: String,
        userId: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val docRef = firestore.collection(Constants.COLLECTION_CHATS).document(messageId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentMessage = snapshot.toObject(ChatMessage::class.java)
                val currentReactions = currentMessage?.reacciones?.toMutableMap() ?: mutableMapOf()

                if (currentReactions[userId] == emoji) {
                    currentReactions.remove(userId)
                } else {
                    currentReactions[userId] = emoji
                }

                transaction.update(docRef, "reacciones", currentReactions)
            }.await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al reaccionar: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun markMessagesAsRead(userId: String) = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_CHATS)
                .limit(50)
                .get()
                .await()

            val batch = firestore.batch()
            var count = 0

            for (doc in snapshot.documents) {
                val leidoPor = doc.get("leidoPor") as? List<*> ?: emptyList<String>()
                if (!leidoPor.contains(userId)) {
                    batch.update(doc.reference, "leidoPor", FieldValue.arrayUnion(userId))
                    count++
                }
            }

            if (count > 0) {
                batch.commit().await()
            }
        } catch (_: Exception) {
        }
    }
}
