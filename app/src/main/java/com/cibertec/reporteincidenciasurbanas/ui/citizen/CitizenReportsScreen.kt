package com.cibertec.reporteincidenciasurbanas.ui.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.utils.DateUtils
import com.cibertec.reporteincidenciasurbanas.utils.ImageUtils
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenReportsViewModel

import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun CitizenReportsScreen(
    viewModel: CitizenReportsViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    CitizenReportsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = { viewModel.loadReports() },
        onExportPdf = {
            viewModel.exportAllReportsPdf(
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
        onFilterSelected = { viewModel.onFilterSelected(it) },
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToCreateReport = onNavigateToCreateReport
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenReportsContent(
    uiState: com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenReportsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRefresh: () -> Unit,
    onExportPdf: () -> Unit = {},
    onFilterSelected: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateReport: () -> Unit
) {
    val tabs = listOf(
        "TODOS" to stringResource(R.string.tab_all),
        IncidentStatus.PENDIENTE.name to stringResource(R.string.tab_pending),
        IncidentStatus.EN_REVISION.name to stringResource(R.string.tab_in_review),
        IncidentStatus.EN_ATENCION.name to stringResource(R.string.tab_in_progress),
        IncidentStatus.RESUELTO.name to stringResource(R.string.tab_resolved)
    )

    val selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedStatusFilter }.coerceAtLeast(0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_reports_title), fontWeight = FontWeight.Bold) },
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
                            enabled = uiState.allIncidents.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = stringResource(R.string.pdf_btn_tooltip_all),
                                tint = if (uiState.allIncidents.isNotEmpty()) PrimaryBlue else Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_refresh)
                        )
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
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onFilterSelected(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) PrimaryBlue else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (uiState.filteredIncidents.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.my_reports_empty_title),
                    subtitle = stringResource(R.string.my_reports_empty_subtitle),
                    icon = Icons.Default.Assignment,
                    actionButtonText = if (uiState.selectedStatusFilter == "TODOS") stringResource(R.string.my_reports_btn_create) else null,
                    onActionClick = onNavigateToCreateReport
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredIncidents, key = { it.id }) { incident ->
                        CitizenIncidentCard(
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
fun CitizenIncidentCard(
    incident: Incident,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
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
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = incident.categoriaNombre.ifBlank { stringResource(R.string.report_item_default_category) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = incident.descripcion.take(50) + if (incident.descripcion.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = incident.direccionReferencial.ifBlank { stringResource(R.string.report_item_default_location) },
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = DateUtils.formatTimestamp(incident.fechaCreacion),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CitizenReportsScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        CitizenReportsContent(
            uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenReportsUiState(
                isLoading = false,
                filteredIncidents = listOf(
                    Incident(
                        id = "1",
                        codigo = "INC-1001",
                        categoriaNombre = "Pistas y Veredas",
                        descripcion = "Hueco profundo en la pista frente al parque",
                        direccionReferencial = "Av. Las Flores 123",
                        estado = "PENDIENTE",
                        usuarioNombre = "Juan Perez"
                    ),
                    Incident(
                        id = "2",
                        codigo = "INC-1002",
                        categoriaNombre = "Alumbrado Público",
                        descripcion = "Poste de luz sin foco desde hace una semana",
                        direccionReferencial = "Jr. Progreso 456",
                        estado = "EN_ATENCION",
                        usuarioNombre = "Juan Perez"
                    )
                )
            ),
            onRefresh = {},
            onFilterSelected = {},
            onNavigateToDetail = {},
            onNavigateToCreateReport = {}
        )
    }
}
