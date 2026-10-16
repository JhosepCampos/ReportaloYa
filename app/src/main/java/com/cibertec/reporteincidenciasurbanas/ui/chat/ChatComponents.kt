package com.cibertec.reporteincidenciasurbanas.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusBadge
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlueDark
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.utils.DateUtils
import com.cibertec.reporteincidenciasurbanas.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.Locale

val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    currentUserId: String,
    onMessageLongClick: (ChatMessage) -> Unit,
    onReactionClick: (String) -> Unit,
    onSharedIncidentClick: (ChatMessage) -> Unit = {}
) {
    val bubbleShape = if (isOwnMessage) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val bubbleBackground = if (isOwnMessage) {
        PrimaryBlue.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val timeString = message.timestamp?.toDate()?.let { date ->
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBackground,
            tonalElevation = if (isOwnMessage) 0.dp else 2.dp,
            shadowElevation = if (isOwnMessage) 0.dp else 1.dp,
            modifier = Modifier
                .widthIn(max = if (message.esReporteCompartido) 330.dp else 300.dp)
                .combinedClickable(
                    onClick = {
                        if (!message.eliminado && message.esReporteCompartido) {
                            onSharedIncidentClick(message)
                        }
                    },
                    onLongClick = {
                        if (!message.eliminado) {
                            onMessageLongClick(message)
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isOwnMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = message.remitenteNombre,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (message.esAdmin) PrimaryBlueDark else PrimaryBlue
                        )

                        if (message.esAdmin) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryBlue,
                                modifier = Modifier.height(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stringResource(R.string.chat_role_admin_badge),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (message.eliminado) {
                    Text(
                        text = "🚫 ${stringResource(R.string.chat_message_deleted)}",
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else if (message.esReporteCompartido) {
                    SharedIncidentCard(
                        message = message,
                        onViewDetailClick = { onSharedIncidentClick(message) },
                        onCardLongClick = { onMessageLongClick(message) }
                    )
                } else {
                    Text(
                        text = message.texto,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.editado && !message.eliminado) {
                        Text(
                            text = stringResource(R.string.chat_edited_badge),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    if (timeString.isNotBlank()) {
                        Text(
                            text = timeString,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        val reactionsCount = message.countReactions()
        if (reactionsCount.isNotEmpty() && !message.eliminado) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onMessageLongClick(message) }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactionsCount.forEach { (emoji, count) ->
                    Text(
                        text = if (count > 1) "$emoji $count" else emoji,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onReactionClick(emoji) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedIncidentCard(
    message: ChatMessage,
    onViewDetailClick: () -> Unit,
    onCardLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onViewDetailClick,
                onLongClick = onCardLongClick
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.shared_report_tag),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Text(
                    text = message.reporteCodigo ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.reporteCategoria ?: stringResource(R.string.report_item_default_category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = message.reporteEstado ?: "PENDIENTE")
                }

                if (!message.reporteFotoUrl.isNullOrBlank()) {
                    val imageModel = remember(message.reporteFotoUrl) {
                        ImageUtils.getImageModel(message.reporteFotoUrl)
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (!message.reporteDescripcion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.reporteDescripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            if (!message.reporteUbicacion.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SecondaryGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.reporteUbicacion,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${stringResource(R.string.shared_report_created_at)} ${DateUtils.formatTimestamp(message.reporteFechaCreacion)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            if (message.reporteFechaActualizacion != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${stringResource(R.string.shared_report_updated_at)} ${DateUtils.formatTimestamp(message.reporteFechaActualizacion)}",
                        fontSize = 10.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FilledTonalButton(
                onClick = onViewDetailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.12f),
                    contentColor = PrimaryBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.shared_report_view_detail),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SharedIncidentDetailDialog(
    message: ChatMessage,
    onDismiss: () -> Unit
) {
    val locationFallback = stringResource(R.string.detail_location_fallback)
    val descText = if (!message.reporteDescripcion.isNullOrBlank()) {
        message.reporteDescripcion
    } else {
        "Sin descripción detallada"
    }
    val locationText = if (!message.reporteUbicacion.isNullOrBlank()) {
        message.reporteUbicacion
    } else {
        locationFallback
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = message.reporteCodigo ?: stringResource(R.string.shared_report_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = message.reporteCategoria ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    StatusBadge(status = message.reporteEstado ?: "PENDIENTE")
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!message.reporteFotoUrl.isNullOrBlank()) {
                    val imageModel = remember(message.reporteFotoUrl) {
                        ImageUtils.getImageModel(message.reporteFotoUrl)
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = stringResource(R.string.detail_photo_cd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.detail_desc_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SecondaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.detail_location_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        if (message.reporteLatitud != 0.0 || message.reporteLongitud != 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.5f, %.5f", message.reporteLatitud, message.reporteLongitud),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.shared_report_created_at)} ${DateUtils.formatTimestamp(message.reporteFechaCreacion)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    if (message.reporteFechaActualizacion != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Update,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${stringResource(R.string.shared_report_updated_at)} ${DateUtils.formatTimestamp(message.reporteFechaActualizacion)}",
                                fontSize = 12.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_close),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsBottomSheet(
    message: ChatMessage,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOwner = message.remitenteId == currentUserId && !message.eliminado

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_reaction_menu_title),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    val isSelected = message.reacciones[currentUserId] == emoji
                    IconButton(
                        onClick = { onReactionSelected(emoji) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }

            if (isOwner) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (!message.esReporteCompartido) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.chat_action_edit)) },
                        leadingContent = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue)
                        },
                        modifier = Modifier.clickable { onEditClick() }
                    )
                }

                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.chat_action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onDeleteClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EditingBanner(
    messageText: String,
    onCancelEdit: () -> Unit
) {
    Surface(
        color = PrimaryBlue.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.chat_editing_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onCancelEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.chat_action_cancel_edit),
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
