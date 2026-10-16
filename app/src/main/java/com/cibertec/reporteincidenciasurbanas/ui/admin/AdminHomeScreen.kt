package com.cibertec.reporteincidenciasurbanas.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.ui.components.EmptyStateView
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusBadge
import com.cibertec.reporteincidenciasurbanas.ui.theme.*
import com.cibertec.reporteincidenciasurbanas.utils.ImageUtils
import com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardUiState
import com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardViewModel

@Composable
fun AdminHomeScreen(
    currentUser: User?,
    viewModel: AdminDashboardViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAllReports: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadIncidents()
    }

    AdminHomeContent(
        currentUser = currentUser,
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToAllReports = onNavigateToAllReports
    )
}

@Composable
fun AdminHomeContent(
    currentUser: User?,
    uiState: AdminDashboardUiState,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAllReports: () -> Unit
) {
    val adminName = currentUser?.nombres?.ifBlank { "Administrador" } ?: "Administrador"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "¡Hola, $adminName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.admin_home_overview_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                    Text(
                        text = stringResource(R.string.admin_home_banner_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.admin_home_banner_subtitle),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onNavigateToAllReports,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.admin_home_btn_manage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = stringResource(R.string.admin_home_status_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminMetricMiniCard(
                title = stringResource(R.string.admin_metric_total),
                count = uiState.totalIncidents,
                color = PrimaryBlue,
                icon = Icons.Default.Assignment,
                modifier = Modifier.weight(1f)
            )
            AdminMetricMiniCard(
                title = stringResource(R.string.admin_metric_pending),
                count = uiState.pendingCount,
                color = AccentOrange,
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f)
            )
            AdminMetricMiniCard(
                title = stringResource(R.string.admin_metric_in_progress),
                count = uiState.inProgressCount,
                color = StatusPendingColor,
                icon = Icons.Default.HourglassTop,
                modifier = Modifier.weight(1f)
            )
            AdminMetricMiniCard(
                title = stringResource(R.string.admin_metric_resolved),
                count = uiState.resolvedCount,
                color = SecondaryGreen,
                icon = Icons.Default.CheckCircle,
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
                text = stringResource(R.string.admin_home_recent_incidents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = onNavigateToAllReports) {
                Text(
                    text = stringResource(R.string.admin_home_btn_view_all),
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val recentList = uiState.incidents.take(4)

        if (recentList.isEmpty()) {
            EmptyStateView(
                title = "No hay incidencias registradas",
                subtitle = "Las incidencias reportadas por la ciudadanía aparecerán aquí."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recentList.forEach { incident ->
                    AdminRecentIncidentItem(
                        incident = incident,
                        onClick = { onNavigateToDetail(incident.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun AdminMetricMiniCard(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AdminRecentIncidentItem(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryBlue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!incident.fotografiaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = remember(incident.fotografiaUrl) { ImageUtils.getImageModel(incident.fotografiaUrl) },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incident.codigo.ifBlank { "INCIDENCIA" },
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 13.sp
                    )
                    StatusBadge(status = incident.estado)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = incident.categoriaNombre.ifBlank { "Incidencia Urbana" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    text = incident.descripcion,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminHomeScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        AdminHomeContent(
            currentUser = User(nombres = "Administrador", apellidos = "Municipal", rol = UserRole.ADMINISTRADOR.name),
            uiState = AdminDashboardUiState(
                totalIncidents = 12,
                pendingCount = 4,
                inProgressCount = 5,
                resolvedCount = 3
            ),
            onNavigateToDetail = {},
            onNavigateToAllReports = {}
        )
    }
}
