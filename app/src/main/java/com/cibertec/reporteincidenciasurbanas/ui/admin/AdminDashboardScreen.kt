package com.cibertec.reporteincidenciasurbanas.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.ui.components.EmptyStateView
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusBadge
import com.cibertec.reporteincidenciasurbanas.ui.theme.AccentOrange
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.ui.theme.StatusPendingColor
import com.cibertec.reporteincidenciasurbanas.utils.ImageUtils
import com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardViewModel

import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onNavigateToDetail: (String) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadIncidents()
    }

    AdminDashboardContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = { viewModel.loadIncidents() },
        onExportPdf = {
            viewModel.exportIncidentsPdf(
                context = context,
                onSuccess = { pdfFile ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.pdf_downloaded_to_folder, pdfFile.name)
                        )
                    }
                    PdfExporter.openPdf(context, pdfFile)
                },
                onError = { errorMsg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errorMsg)
                    }
                }
            )
        },
        onStatusFilterChanged = { viewModel.onStatusFilterChanged(it) },
        onCategoryFilterChanged = { viewModel.onCategoryFilterChanged(it) },
        onNavigateToDetail = onNavigateToDetail,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardContent(
    uiState: com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRefresh: () -> Unit,
    onExportPdf: () -> Unit = {},
    onStatusFilterChanged: (String) -> Unit,
    onCategoryFilterChanged: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onLogout: (() -> Unit)? = null
) {
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "TODOS" to stringResource(R.string.status_all_states),
        IncidentStatus.PENDIENTE.name to stringResource(R.string.tab_pending),
        IncidentStatus.EN_REVISION.name to stringResource(R.string.tab_in_review),
        IncidentStatus.EN_ATENCION.name to stringResource(R.string.tab_in_progress),
        IncidentStatus.RESUELTO.name to stringResource(R.string.tab_resolved)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard_title), fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.isExportingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = PrimaryBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onExportPdf,
                            enabled = uiState.incidents.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.pdf_btn_tooltip_all),
                                tint = if (uiState.incidents.isNotEmpty()) PrimaryBlue else Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_refresh)
                        )
                    }
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = stringResource(R.string.btn_exit),
                                tint = Color(0xFFD32F2F)
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminMetricCard(
                    title = stringResource(R.string.admin_metric_total),
                    count = uiState.totalIncidents,
                    icon = Icons.Default.Assignment,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = stringResource(R.string.admin_metric_pending),
                    count = uiState.pendingCount,
                    icon = Icons.Default.PendingActions,
                    color = StatusPendingColor,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = stringResource(R.string.admin_metric_in_progress),
                    count = uiState.inProgressCount,
                    icon = Icons.Default.HourglassTop,
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = stringResource(R.string.admin_metric_resolved),
                    count = uiState.resolvedCount,
                    icon = Icons.Default.CheckCircle,
                    color = SecondaryGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = statusOptions.find { it.first == uiState.selectedStatusFilter }?.second ?: stringResource(R.string.admin_filter_status_label),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.admin_filter_status_label), fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    onStatusFilterChanged(option.first)
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentCategoryName = if (uiState.selectedCategoryFilter == "TODAS") stringResource(R.string.tab_all) else
                        uiState.categories.find { it.id == uiState.selectedCategoryFilter }?.nombre ?: stringResource(R.string.admin_filter_category_label)

                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.admin_filter_category_label), fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.admin_filter_all_categories)) },
                            onClick = {
                                onCategoryFilterChanged("TODAS")
                                categoryDropdownExpanded = false
                            }
                        )
                        uiState.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    onCategoryFilterChanged(cat.id)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (uiState.incidents.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.admin_empty_title),
                    subtitle = stringResource(R.string.admin_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.incidents, key = { it.id }) { incident ->
                        AdminIncidentCard(
                            incident = incident,
                            onClick = { onNavigateToDetail(incident.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 16.sp,
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
fun AdminIncidentCard(
    incident: Incident,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val imageModel = remember(incident.fotografiaUrl) {
                    ImageUtils.getImageModel(incident.fotografiaUrl)
                }
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = stringResource(R.string.report_item_photo_cd),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(28.dp)
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
                        text = incident.codigo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    StatusBadge(status = incident.estado)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = incident.categoriaNombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = incident.descripcion.take(45) + if (incident.descripcion.length > 45) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = incident.direccionReferencial.ifBlank { stringResource(R.string.report_item_default_location) },
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminDashboardScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        AdminDashboardContent(
            uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardUiState(
                totalIncidents = 12,
                pendingCount = 4,
                inProgressCount = 5,
                resolvedCount = 3,
                incidents = listOf(
                    Incident(
                        id = "1",
                        codigo = "INC-20386",
                        categoriaNombre = "Fugas de agua",
                        descripcion = "Se rompió una tubería principal",
                        direccionReferencial = "Pasaje Cahuide 123",
                        estado = "PENDIENTE",
                        usuarioNombre = "Jhosep Campos"
                    )
                )
            ),
            onRefresh = {},
            onStatusFilterChanged = {},
            onCategoryFilterChanged = {},
            onNavigateToDetail = {},
            onLogout = {}
        )
    }
}
