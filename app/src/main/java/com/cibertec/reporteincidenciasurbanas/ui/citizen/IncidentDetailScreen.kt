package com.cibertec.reporteincidenciasurbanas.ui.citizen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.cibertec.reporteincidenciasurbanas.data.model.StatusHistoryEntry
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusBadge
import com.cibertec.reporteincidenciasurbanas.ui.components.StatusTimeline
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.utils.DateUtils
import com.cibertec.reporteincidenciasurbanas.utils.ImageUtils
import com.cibertec.reporteincidenciasurbanas.viewmodel.IncidentDetailViewModel
import java.util.Locale

import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun IncidentDetailScreen(
    incidentId: String,
    viewModel: IncidentDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(incidentId) {
        viewModel.loadIncident(incidentId)
    }

    IncidentDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onExportPdf = {
            viewModel.exportIncidentPdf(
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
                        snackbarHostState.showSnackbar(message = errorMsg)
                    }
                }
            )
        },
        onShareToChat = {
            viewModel.shareIncidentToCommunityChat(
                context = context,
                onSuccess = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.share_to_chat_success)
                        )
                    }
                },
                onError = { errorMsg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = errorMsg)
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailContent(
    uiState: com.cibertec.reporteincidenciasurbanas.viewmodel.IncidentDetailUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onExportPdf: () -> Unit = {},
    onShareToChat: () -> Unit = {}
) {
    val incident = uiState.incident

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(incident?.codigo ?: stringResource(R.string.detail_title_fallback), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    if (incident != null) {
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
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = stringResource(R.string.pdf_btn_tooltip_single),
                                    tint = PrimaryBlue
                                )
                            }
                        }

                        if (uiState.isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = onShareToChat,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.share_to_chat_tooltip),
                                    tint = PrimaryBlue
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (incident == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.detail_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = incident.categoriaNombre,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = DateUtils.formatTimestamp(incident.fechaCreacion),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    StatusBadge(status = incident.estado)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (incident.fotografiaUrl.isNotBlank()) {
                    val imageModel = remember(incident.fotografiaUrl) {
                        ImageUtils.getImageModel(incident.fotografiaUrl)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = stringResource(R.string.detail_photo_cd),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.detail_desc_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = incident.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SecondaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.detail_location_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = incident.direccionReferencial.ifBlank { stringResource(R.string.detail_location_fallback) },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.detail_coordinates_format,
                                    String.format(Locale.US, "%.5f", incident.latitud),
                                    String.format(Locale.US, "%.5f", incident.longitud)
                                ),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StatusTimeline(
                    currentStatus = incident.estado,
                    history = incident.historialEstados
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun IncidentDetailScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        IncidentDetailContent(
            uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.IncidentDetailUiState(
                incident = Incident(
                    id = "1",
                    codigo = "INC-20386",
                    categoriaNombre = "Fugas de agua",
                    descripcion = "Se rompió una tubería principal y está inundando la vía pública.",
                    direccionReferencial = "Pasaje Cahuide, A.h Santa Rosa Baja",
                    latitud = -12.046374,
                    longitud = -77.042793,
                    estado = "PENDIENTE",
                    usuarioNombre = "Jhosep Campos",
                    historialEstados = listOf(
                        StatusHistoryEntry(
                            estado = "PENDIENTE",
                            observacion = "Incidencia registrada por el ciudadano.",
                            nombreAdmin = "Jhosep Campos"
                        )
                    )
                )
            ),
            onNavigateBack = {}
        )
    }
}
