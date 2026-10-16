package com.cibertec.reporteincidenciasurbanas.ui.citizen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Category
import com.cibertec.reporteincidenciasurbanas.ui.components.LoadingDialog
import com.cibertec.reporteincidenciasurbanas.ui.components.MapPreviewCard
import com.cibertec.reporteincidenciasurbanas.ui.components.PhotoPicker
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.ui.theme.SecondaryGreen
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.PermissionUtils
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.cibertec.reporteincidenciasurbanas.viewmodel.CreateReportViewModel

@Composable
fun CreateReportScreen(
    viewModel: CreateReportViewModel,
    onNavigateBack: () -> Unit,
    onReportSubmittedSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            viewModel.fetchLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasLocation = PermissionUtils.hasLocationPermission(context)
        val hasCamera = PermissionUtils.hasCameraPermission(context)

        if (!hasLocation || !hasCamera) {
            val permissionsNeeded = mutableListOf<String>()
            if (!hasLocation) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!hasCamera) {
                permissionsNeeded.add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    LaunchedEffect(uiState.submissionResult) {
        when (val result = uiState.submissionResult) {
            is Resource.Success -> {
                val incidentId = result.data
                viewModel.resetSubmission()
                onReportSubmittedSuccess(incidentId)
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(result.message)
            }
            else -> {}
        }
    }

    CreateReportContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onCategorySelected = { viewModel.onCategorySelected(it) },
        onDescriptionChanged = { viewModel.onDescriptionChanged(it) },
        onPhotoSelected = { viewModel.onPhotoSelected(it) },
        onRefreshLocation = { viewModel.fetchLocation() },
        onSubmitReport = { viewModel.submitReport() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateReportContent(
    uiState: com.cibertec.reporteincidenciasurbanas.viewmodel.CreateReportUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPhotoSelected: (android.net.Uri?) -> Unit,
    onRefreshLocation: () -> Unit,
    onSubmitReport: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_report_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
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
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.create_report_step1_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.create_report_step1_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.categories.forEach { category ->
                            val isSelected = uiState.selectedCategory?.id == category.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category.nombre, fontSize = 13.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
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
                    Text(
                        text = stringResource(R.string.create_report_step2_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.create_report_step2_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { onDescriptionChanged(it) },
                        placeholder = { Text(stringResource(R.string.create_report_desc_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (uiState.description.trim().length < Constants.MIN_DESCRIPTION_LENGTH)
                                        stringResource(R.string.create_report_desc_min_chars) else stringResource(R.string.create_report_desc_valid),
                                    color = if (uiState.description.trim().length < Constants.MIN_DESCRIPTION_LENGTH)
                                        Color.Gray else SecondaryGreen
                                )
                                Text("${uiState.description.length}/${Constants.MAX_DESCRIPTION_LENGTH}")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PhotoPicker(
                selectedPhotoUri = uiState.photoUri,
                onPhotoSelected = { onPhotoSelected(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            MapPreviewCard(
                coordinates = uiState.coordinates,
                isLoading = uiState.isGpsLoading,
                onRefreshLocation = { onRefreshLocation() }
            )

            if (uiState.validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.validationError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSubmitReport() },
                enabled = uiState.isFormValid && !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGreen,
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.create_report_btn_submit),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.isSubmitting) {
        LoadingDialog(message = uiState.submissionStatusText.ifBlank { stringResource(R.string.create_report_submitting_msg) })
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateReportScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        CreateReportContent(
            uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.CreateReportUiState(
                categories = listOf(
                    Category("baches", "Baches y vías"),
                    Category("alumbrado", "Alumbrado"),
                    Category("basura", "Basura")
                ),
                selectedCategory = Category("baches", "Baches y vías"),
                description = "Bache profundo en la avenida principal frente a la escuela."
            ),
            onNavigateBack = {},
            onCategorySelected = {},
            onDescriptionChanged = {},
            onPhotoSelected = {},
            onRefreshLocation = {},
            onSubmitReport = {}
        )
    }
}
