package com.cibertec.reporteincidenciasurbanas.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.ui.components.EmptyStateView
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatUiState
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect

@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.setChatScreenActive(true)
        onDispose {
            viewModel.setChatScreenActive(false)
        }
    }

    ChatContent(
        uiState = uiState,
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onSendMessage = { viewModel.sendMessage() },
        onMessageSelectForOptions = { viewModel.setSelectedMessageForOptions(it) },
        onDismissOptions = { viewModel.setSelectedMessageForOptions(null) },
        onReactionSelected = { emoji ->
            uiState.selectedMessageForOptions?.let { msg ->
                viewModel.toggleReaction(msg.id, emoji)
            }
        },
        onStartEditMessage = { msg -> viewModel.startEditingMessage(msg) },
        onCancelEdit = { viewModel.cancelEditing() },
        onRequestDeleteMessage = { msg -> viewModel.requestDeleteMessage(msg) },
        onConfirmDelete = { viewModel.confirmDeleteMessage() },
        onDismissDeleteDialog = { viewModel.dismissDeleteDialog() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    uiState: ChatUiState,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMessageSelectForOptions: (ChatMessage) -> Unit,
    onDismissOptions: () -> Unit,
    onReactionSelected: (String) -> Unit,
    onStartEditMessage: (ChatMessage) -> Unit,
    onCancelEdit: () -> Unit,
    onRequestDeleteMessage: (ChatMessage) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = uiState.currentUser?.id ?: ""
    var selectedSharedIncident by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.chat_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.chat_subtitle_online),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue
                    )
                } else if (uiState.messages.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = stringResource(R.string.chat_empty_title),
                        subtitle = stringResource(R.string.chat_empty_subtitle)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                isOwnMessage = message.remitenteId == currentUserId,
                                currentUserId = currentUserId,
                                onMessageLongClick = { onMessageSelectForOptions(it) },
                                onReactionClick = { emoji ->
                                    onReactionSelected(emoji)
                                },
                                onSharedIncidentClick = { sharedMsg ->
                                    selectedSharedIncident = sharedMsg
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.editingMessage != null) {
                EditingBanner(
                    messageText = uiState.editingMessage.texto,
                    onCancelEdit = onCancelEdit
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = {
                            Text(
                                stringResource(R.string.chat_input_placeholder),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    IconButton(
                        onClick = {
                            if (uiState.inputText.isNotBlank() && !uiState.isSending) {
                                onSendMessage()
                                coroutineScope.launch {
                                    if (uiState.messages.isNotEmpty()) {
                                        listState.animateScrollToItem(uiState.messages.size - 1)
                                    }
                                }
                            }
                        },
                        enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (uiState.inputText.isNotBlank()) PrimaryBlue else Color.LightGray,
                                shape = CircleShape
                            )
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSharedIncident?.let { sharedMessage ->
        SharedIncidentDetailDialog(
            message = sharedMessage,
            onDismiss = { selectedSharedIncident = null }
        )
    }

    if (uiState.selectedMessageForOptions != null) {
        MessageOptionsBottomSheet(
            message = uiState.selectedMessageForOptions,
            currentUserId = currentUserId,
            onDismiss = onDismissOptions,
            onReactionSelected = onReactionSelected,
            onEditClick = {
                onStartEditMessage(uiState.selectedMessageForOptions)
            },
            onDeleteClick = {
                onRequestDeleteMessage(uiState.selectedMessageForOptions)
            }
        )
    }

    if (uiState.showDeleteConfirmDialog && uiState.messageToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text(stringResource(R.string.chat_delete_dialog_title)) },
            text = { Text(stringResource(R.string.chat_delete_dialog_msg)) },
            confirmButton = {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.chat_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChatScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        ChatContent(
            uiState = ChatUiState(
                isLoading = false,
                currentUser = User(id = "user1", nombres = "Jhosep", apellidos = "Campos", rol = UserRole.CIUDADANO.name),
                messages = listOf(
                    ChatMessage(
                        id = "1",
                        remitenteId = "admin1",
                        remitenteNombre = "Mesa Municipal",
                        remitenteRol = UserRole.ADMINISTRADOR.name,
                        texto = "Estimados vecinos, hoy se iniciaron las reparaciones en la Av. Las Flores.",
                        reacciones = mapOf("user1" to "👍", "user2" to "❤️")
                    ),
                    ChatMessage(
                        id = "2",
                        remitenteId = "user1",
                        remitenteNombre = "Jhosep Campos",
                        remitenteRol = UserRole.CIUDADANO.name,
                        texto = "Excelente noticia, muchas gracias por la pronta atención.",
                        reacciones = mapOf("admin1" to "🙏")
                    )
                )
            ),
            onInputTextChanged = {},
            onSendMessage = {},
            onMessageSelectForOptions = {},
            onDismissOptions = {},
            onReactionSelected = {},
            onStartEditMessage = {},
            onCancelEdit = {},
            onRequestDeleteMessage = {},
            onConfirmDelete = {},
            onDismissDeleteDialog = {}
        )
    }
}
