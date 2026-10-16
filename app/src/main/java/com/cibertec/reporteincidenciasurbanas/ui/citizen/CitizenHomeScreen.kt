package com.cibertec.reporteincidenciasurbanas.ui.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusBadge
import com.cibertec.reporteincidenciasurbanas.ui.theme.AccentOrange
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlueDark
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.utils.DateUtils
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeViewModel

@Composable
fun CitizenHomeScreen(
    currentUser: User?,
    viewModel: CitizenHomeViewModel,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToIncidentDetail: (String) -> Unit,
    onNavigateToAllReports: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    CitizenHomeContent(
        currentUser = currentUser,
        uiState = uiState,
        onNavigateToCreateReport = onNavigateToCreateReport,
        onNavigateToIncidentDetail = onNavigateToIncidentDetail,
        onNavigateToAllReports = onNavigateToAllReports
    )
}

@Composable
fun CitizenHomeContent(
    currentUser: User?,
    uiState: com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeUiState,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToIncidentDetail: (String) -> Unit,
    onNavigateToAllReports: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fallbackName = stringResource(R.string.home_greeting_fallback)
            val displayName = currentUser?.nombres?.ifBlank { fallbackName } ?: fallbackName
            Column {
                Text(
                    text = stringResource(R.string.home_greeting_prefix, displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.home_community_slogan),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onNavigateToCreateReport() },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBlueDark, PrimaryBlue)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_banner_subtitle),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.home_banner_title),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocationAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToCreateReport,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_banner_title),
                            color = PrimaryBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryBlueDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.home_my_stats_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = stringResource(R.string.home_stat_total),
                count = uiState.totalReports,
                icon = Icons.Default.Assignment,
                iconTint = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = stringResource(R.string.home_stat_in_progress),
                count = uiState.inProgressReports,
                icon = Icons.Default.HourglassTop,
                iconTint = AccentOrange,
                modifier = Modifier.weight(1f)
            )
            StatMetricCard(
                title = stringResource(R.string.home_stat_resolved),
                count = uiState.resolvedReports,
                icon = Icons.Default.CheckCircle,
                iconTint = SecondaryGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_recent_reports_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (uiState.recentIncidents.isNotEmpty()) {
                TextButton(onClick = onNavigateToAllReports) {
                    Text(stringResource(R.string.home_btn_see_all), color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.recentIncidents.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_empty_recent),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            uiState.recentIncidents.forEach { incident ->
                RecentIncidentItem(
                    incident = incident,
                    onClick = { onNavigateToIncidentDetail(incident.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun RecentIncidentItem(
    incident: Incident,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.categoriaNombre.ifBlank { stringResource(R.string.report_item_default_category) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = incident.descripcion.take(45) + if (incident.descripcion.length > 45) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${incident.codigo} • ${DateUtils.formatShortDate(incident.fechaCreacion)}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
            StatusBadge(status = incident.estado)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CitizenHomeScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        CitizenHomeContent(
            currentUser = User(nombres = "Jhosep", apellidos = "Campos"),
            uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeUiState(
                totalReports = 3,
                inProgressReports = 1,
                resolvedReports = 2,
                recentIncidents = listOf(
                    Incident(
                        id = "1",
                        codigo = "INC-20386",
                        categoriaNombre = "Fugas de agua",
                        descripcion = "Tubería rota en vía pública",
                        estado = "RESUELTO"
                    )
                )
            ),
            onNavigateToCreateReport = {},
            onNavigateToIncidentDetail = {},
            onNavigateToAllReports = {}
        )
    }
}
