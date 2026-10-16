package com.cibertec.reporteincidenciasurbanas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.data.model.StatusHistoryEntry
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.utils.DateUtils

@Composable
fun StatusTimeline(
    currentStatus: String,
    history: List<StatusHistoryEntry>,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        IncidentStatus.PENDIENTE,
        IncidentStatus.EN_REVISION,
        IncidentStatus.EN_ATENCION,
        IncidentStatus.RESUELTO
    )

    val currentStatusEnum = IncidentStatus.fromString(currentStatus)
    val currentIndex = steps.indexOf(currentStatusEnum).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_timeline_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, step ->
                val isCompleted = index <= currentIndex
                val isCurrent = index == currentIndex
                val historyEntry = history.find { it.estado == step.name }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> PrimaryBlue
                                        isCompleted -> SecondaryGreen
                                        else -> Color(0xFFE0E0E0)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }

                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(44.dp)
                                    .background(
                                        if (index < currentIndex) SecondaryGreen else Color(0xFFE0E0E0)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index < steps.size - 1) 16.dp else 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = step.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                modifier = Modifier.weight(1f)
                            )

                            if (historyEntry?.fecha != null) {
                                Text(
                                    text = DateUtils.formatTimestamp(historyEntry.fecha),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (historyEntry != null && historyEntry.observacion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = historyEntry.observacion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusTimelinePreview() {
    ReporteIncidenciasUrbanasTheme {
        StatusTimeline(
            currentStatus = IncidentStatus.EN_ATENCION.name,
            history = listOf(
                StatusHistoryEntry(
                    estado = IncidentStatus.PENDIENTE.name,
                    observacion = "Reporte enviado por el ciudadano",
                    nombreAdmin = "Jhosep Campos"
                ),
                StatusHistoryEntry(
                    estado = IncidentStatus.EN_REVISION.name,
                    observacion = "Validado por mesa de partes",
                    nombreAdmin = "Mesa de Partes"
                ),
                StatusHistoryEntry(
                    estado = IncidentStatus.EN_ATENCION.name,
                    observacion = "Cuadrilla técnica en camino",
                    nombreAdmin = "Operaciones"
                )
            )
        )
    }
}
