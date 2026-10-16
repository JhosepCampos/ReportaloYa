package com.cibertec.reporteincidenciasurbanas.version

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen

@Composable
fun VersionUpdateDialog(
    uiState: VersionUiState,
    onUpdateClick: () -> Unit,
    onRetryClick: () -> Unit = onUpdateClick
) {
    if (!uiState.isUpdateRequired && !uiState.isDownloading && uiState.errorMessage == null) {
        return
    }

    Dialog(
        onDismissRequest = { /* Bloqueante: No permite cerrar tocando afuera */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Icono decorativo con contenedor circular Material 3
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isDownloading) Icons.Default.CloudDownload else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Título
                Text(
                    text = if (uiState.isDownloading) {
                        stringResource(R.string.version_update_downloading_title)
                    } else {
                        stringResource(R.string.version_update_dialog_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Subtítulo / Descripción
                Text(
                    text = if (uiState.isDownloading) {
                        stringResource(R.string.version_update_downloading_subtitle)
                    } else {
                        stringResource(R.string.version_update_dialog_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Estado de Descarga en Progreso (Diálogo Actualizando)
                if (uiState.isDownloading || uiState.isInstalling) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.downloadProgressPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = PrimaryBlue,
                            trackColor = PrimaryBlue.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (uiState.isInstalling) {
                                stringResource(R.string.version_update_installing_msg)
                            } else {
                                stringResource(R.string.version_update_progress_format, uiState.downloadProgressPercentage)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                } else {
                    // 5. Comparativa de Versiones (Instalada vs Nueva)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.version_update_current_label),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "v${uiState.currentVersionName} (${uiState.currentVersionCode})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.version_update_latest_label),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "v${uiState.config?.latestVersionName ?: "1.0.0"} (${uiState.config?.latestVersionCode ?: uiState.currentVersionCode})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6. Novedades / Release Notes
                    val rawNotes = uiState.config?.releaseNotes?.ifBlank {
                        stringResource(R.string.version_update_release_notes_default)
                    } ?: stringResource(R.string.version_update_release_notes_default)
                    val notes = rawNotes.replace("\\n", "\n").replace("\\r", "")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.version_update_release_notes_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Mensaje de Error si ocurrió alguno
                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. Botón Principal de Actualizar
                    Button(
                        onClick = if (uiState.errorMessage != null) onRetryClick else onUpdateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(
                            text = if (uiState.errorMessage != null) {
                                stringResource(R.string.version_update_retry_btn)
                            } else {
                                stringResource(R.string.version_update_btn_update)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VersionUpdateDialogPromptPreview() {
    ReporteIncidenciasUrbanasTheme {
        VersionUpdateDialog(
            uiState = VersionUiState(
                isUpdateRequired = true,
                currentVersionCode = 1,
                currentVersionName = "1.0.0",
                config = AppVersionConfig(
                    minVersionCode = 2,
                    latestVersionCode = 2,
                    latestVersionName = "2.0.0",
                    forceUpdate = true,
                    releaseNotes = "• Nuevo sistema de reportes en PDF.\n• Notificaciones automáticas de chat.\n• Corrección de errores."
                )
            ),
            onUpdateClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VersionUpdateDialogDownloadingPreview() {
    ReporteIncidenciasUrbanasTheme {
        VersionUpdateDialog(
            uiState = VersionUiState(
                isUpdateRequired = true,
                isDownloading = true,
                downloadProgressPercentage = 68
            ),
            onUpdateClick = {}
        )
    }
}
