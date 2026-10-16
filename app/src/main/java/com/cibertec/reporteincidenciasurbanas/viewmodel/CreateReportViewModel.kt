package com.cibertec.reporteincidenciasurbanas.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.data.model.Category
import com.cibertec.reporteincidenciasurbanas.data.model.Coordinates
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.CategoryRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.CategoryRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.LocationRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.LocationRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.MediaRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.MediaRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateReportUiState(
    val categories: List<Category> = Category.getInitialCategories(),
    val selectedCategory: Category? = Category.getInitialCategories().firstOrNull(),
    val description: String = "",
    val photoUri: Uri? = null,
    val coordinates: Coordinates? = null,
    val isGpsLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionStatusText: String = "",
    val submissionResult: Resource<String> = Resource.Idle,
    val validationError: String? = null
) {
    val isFormValid: Boolean
        get() = selectedCategory != null &&
                description.trim().length >= Constants.MIN_DESCRIPTION_LENGTH &&
                photoUri != null &&
                coordinates != null && coordinates.isValid
}

class CreateReportViewModel(
    private val context: Context,
    private val categoryRepository: CategoryRepository = CategoryRepositoryImpl(),
    private val locationRepository: LocationRepository = LocationRepositoryImpl(context),
    private val mediaRepository: MediaRepository = MediaRepositoryImpl(context),
    private val incidentRepository: IncidentRepository = IncidentRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        fetchLocation()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories().collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.value = _uiState.value.copy(
                        categories = resource.data,
                        selectedCategory = resource.data.firstOrNull()
                    )
                }
            }
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category, validationError = null)
    }

    fun onDescriptionChanged(text: String) {
        if (text.length <= Constants.MAX_DESCRIPTION_LENGTH) {
            _uiState.value = _uiState.value.copy(description = text, validationError = null)
        }
    }

    fun onPhotoSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(photoUri = uri, validationError = null)
    }

    fun fetchLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGpsLoading = true)
            locationRepository.getCurrentCoordinates().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            coordinates = resource.data,
                            isGpsLoading = false,
                            validationError = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isGpsLoading = false,
                            validationError = "GPS: ${resource.message}"
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun submitReport() {
        val state = _uiState.value
        val category = state.selectedCategory
        val description = state.description.trim()
        val photoUri = state.photoUri
        val coordinates = state.coordinates
        val uid = authRepository.getCurrentUserUid()

        if (category == null) {
            _uiState.value = _uiState.value.copy(validationError = context.getString(R.string.create_report_error_select_category))
            return
        }
        if (description.length < Constants.MIN_DESCRIPTION_LENGTH) {
            _uiState.value = _uiState.value.copy(validationError = context.getString(R.string.create_report_error_min_desc))
            return
        }
        if (photoUri == null) {
            _uiState.value = _uiState.value.copy(validationError = context.getString(R.string.create_report_error_attach_photo))
            return
        }
        if (coordinates == null || !coordinates.isValid) {
            _uiState.value = _uiState.value.copy(validationError = context.getString(R.string.create_report_error_invalid_gps))
            return
        }
        if (uid == null) {
            _uiState.value = _uiState.value.copy(validationError = context.getString(R.string.create_report_error_invalid_session))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                submissionStatusText = context.getString(R.string.create_report_status_compressing),
                submissionResult = Resource.Loading
            )

            try {
                val compressedBytes = mediaRepository.compressImage(photoUri)

                var authorName = "Ciudadano"
                try {
                    val userRes = userRepository.getUserProfile(uid).firstOrNull { it !is Resource.Loading }
                    if (userRes is Resource.Success) {
                        val fullName = "${userRes.data.nombres} ${userRes.data.apellidos}".trim()
                        if (fullName.isNotBlank()) {
                            authorName = fullName
                        }
                    }
                } catch (e: Exception) {
                }

                _uiState.value = _uiState.value.copy(
                    isSubmitting = true,
                    submissionStatusText = context.getString(R.string.create_report_status_uploading)
                )

                val uploadRes = mediaRepository.uploadIncidentPhoto(compressedBytes, category.id, uid).firstOrNull { it !is Resource.Loading }
                if (uploadRes is Resource.Error) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionResult = Resource.Error(uploadRes.message)
                    )
                    return@launch
                }

                val photoUrl = (uploadRes as? Resource.Success)?.data ?: throw Exception("No se pudo procesar la fotografía")

                _uiState.value = _uiState.value.copy(
                    isSubmitting = true,
                    submissionStatusText = context.getString(R.string.create_report_status_registering)
                )

                val incident = Incident(
                    usuarioId = uid,
                    usuarioNombre = authorName,
                    categoriaId = category.id,
                    categoriaNombre = category.nombre,
                    descripcion = description,
                    fotografiaUrl = photoUrl,
                    latitud = coordinates.latitude,
                    longitud = coordinates.longitude,
                    direccionReferencial = coordinates.address
                )

                val createRes = incidentRepository.createIncident(incident).firstOrNull { it !is Resource.Loading }
                if (createRes is Resource.Success) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionResult = createRes
                    )
                } else if (createRes is Resource.Error) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submissionResult = createRes
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submissionResult = Resource.Error("Error al procesar el reporte: ${e.localizedMessage}")
                )
            }
        }
    }

    fun resetSubmission() {
        _uiState.value = _uiState.value.copy(
            submissionResult = Resource.Idle,
            isSubmitting = false
        )
    }
}
