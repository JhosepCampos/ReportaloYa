package com.cibertec.reporteincidenciasurbanas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.NotificationHelper
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val editingMessage: ChatMessage? = null,
    val selectedMessageForOptions: ChatMessage? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val messageToDelete: ChatMessage? = null,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val isChatScreenActive: Boolean = false
)

class ChatViewModel @JvmOverloads constructor(
    application: Application,
    private val chatRepository: ChatRepository = ChatRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val unreadCount: StateFlow<Int> = _uiState.map { state ->
        val currentUserId = state.currentUser?.id ?: authRepository.getCurrentUserUid() ?: ""
        if (currentUserId.isBlank()) 0
        else {
            state.messages.count { msg ->
                msg.remitenteId != currentUserId && !msg.leidoPor.contains(currentUserId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadCurrentUserAndListenMessages()
    }

    private fun loadCurrentUserAndListenMessages() {
        val uid = authRepository.getCurrentUserUid()
        if (uid != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(uid).collect { resource ->
                    if (resource is Resource.Success) {
                        _uiState.value = _uiState.value.copy(currentUser = resource.data)
                    }
                }
            }
        }

        viewModelScope.launch {
            chatRepository.getMessages().collect { messagesList ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = messagesList
                )
            }
        }
    }

    fun setChatScreenActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(isChatScreenActive = isActive)
        com.cibertec.reporteincidenciasurbanas.utils.ChatNotificationManager.setChatScreenActive(isActive)
        if (isActive) {
            markMessagesAsRead()
            NotificationHelper.clearNotification(getApplication())
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText, errorMessage = null)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val user = _uiState.value.currentUser
        val uid = user?.id ?: authRepository.getCurrentUserUid() ?: return
        val authorName = user?.nombreCompleto?.ifBlank { user.correo } ?: "Usuario"
        val authorRole = user?.rol ?: UserRole.CIUDADANO.name

        val editing = _uiState.value.editingMessage
        if (editing != null) {
            confirmEditMessage(editing.id, text)
            return
        }

        val message = ChatMessage(
            remitenteId = uid,
            remitenteNombre = authorName,
            remitenteRol = authorRole,
            texto = text
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inputText = "", isSending = true)
            chatRepository.sendMessage(message).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isSending = false)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun startEditingMessage(message: ChatMessage) {
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid()
        if (message.remitenteId == currentUserId && !message.eliminado) {
            _uiState.value = _uiState.value.copy(
                editingMessage = message,
                inputText = message.texto,
                selectedMessageForOptions = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            editingMessage = null,
            inputText = ""
        )
    }

    private fun confirmEditMessage(messageId: String, newText: String) {
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            chatRepository.editMessage(messageId, newText, currentUserId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            editingMessage = null,
                            inputText = ""
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun requestDeleteMessage(message: ChatMessage) {
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid()
        if (message.remitenteId == currentUserId && !message.eliminado) {
            _uiState.value = _uiState.value.copy(
                selectedMessageForOptions = null,
                showDeleteConfirmDialog = true,
                messageToDelete = message
            )
        }
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            messageToDelete = null
        )
    }

    fun confirmDeleteMessage() {
        val message = _uiState.value.messageToDelete ?: return
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false, messageToDelete = null)
            chatRepository.deleteMessage(message.id, currentUserId).collect { resource ->
                if (resource is Resource.Error) {
                    _uiState.value = _uiState.value.copy(errorMessage = resource.message)
                }
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedMessageForOptions = null)
            chatRepository.toggleReaction(messageId, emoji, currentUserId).collect { resource ->
                if (resource is Resource.Error) {
                    _uiState.value = _uiState.value.copy(errorMessage = resource.message)
                }
            }
        }
    }

    fun setSelectedMessageForOptions(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(selectedMessageForOptions = message)
    }

    fun markMessagesAsRead() {
        val currentUserId = _uiState.value.currentUser?.id ?: authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(currentUserId)
        }
    }
}
