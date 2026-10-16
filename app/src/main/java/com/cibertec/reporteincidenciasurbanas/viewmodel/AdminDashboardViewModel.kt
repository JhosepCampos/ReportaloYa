package com.cibertec.reporteincidenciasurbanas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.Category
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.CategoryRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.CategoryRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val totalIncidents: Int = 0,
    val pendingCount: Int = 0,
    val inProgressCount: Int = 0,
    val resolvedCount: Int = 0,
    val selectedStatusFilter: String = "TODOS",
    val selectedCategoryFilter: String = "TODAS",
    val categories: List<Category> = emptyList(),
    val incidents: List<Incident> = emptyList(),
    val statusUpdateState: Resource<Unit> = Resource.Idle,
    val errorMessage: String? = null
)

class AdminDashboardViewModel(
    private val incidentRepository: IncidentRepository = IncidentRepositoryImpl(),
    private val categoryRepository: CategoryRepository = CategoryRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadIncidents()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories().collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.value = _uiState.value.copy(categories = resource.data)
                }
            }
        }
    }

    fun loadIncidents() {
        viewModelScope.launch {
            val status = _uiState.value.selectedStatusFilter
            val category = _uiState.value.selectedCategoryFilter

            incidentRepository.getAllIncidents(status, category).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val list = resource.data
                        val total = list.size
                        val pending = list.count { it.estado == IncidentStatus.PENDIENTE.name }
                        val inProgress = list.count {
                            it.estado == IncidentStatus.EN_REVISION.name || it.estado == IncidentStatus.EN_ATENCION.name
                        }
                        val resolved = list.count { it.estado == IncidentStatus.RESUELTO.name }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            totalIncidents = total,
                            pendingCount = pending,
                            inProgressCount = inProgress,
                            resolvedCount = resolved,
                            incidents = list,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun onStatusFilterChanged(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        loadIncidents()
    }

    fun onCategoryFilterChanged(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = categoryId)
        loadIncidents()
    }

    fun updateIncidentStatus(
        incidentId: String,
        newStatus: String,
        observation: String?,
        adminName: String = "Administrador Municipal"
    ) {
        val adminId = authRepository.getCurrentUserUid() ?: "ADMIN_OP"
        viewModelScope.launch {
            incidentRepository.updateIncidentStatus(
                incidentId = incidentId,
                newStatus = newStatus,
                adminId = adminId,
                adminName = adminName,
                observation = observation
            ).collect { resource ->
                _uiState.value = _uiState.value.copy(statusUpdateState = resource)
                if (resource is Resource.Success) {
                    loadIncidents()
                }
            }
        }
    }

    fun exportIncidentsPdf(
        context: Context,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val incidents = _uiState.value.incidents
        if (incidents.isEmpty()) {
            onError(context.getString(R.string.pdf_empty_list_admin))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingPdf = true)
            try {
                val uid = authRepository.getCurrentUserUid()
                var adminUser: User? = null
                if (uid != null) {
                    val userRes = userRepository.getUserProfile(uid).firstOrNull { it is Resource.Success }
                    if (userRes is Resource.Success) {
                        adminUser = userRes.data
                    }
                }

                val pdfFile = withContext(Dispatchers.IO) {
                    PdfExporter.generateAllIncidentsPdf(context, incidents, adminUser)
                }

                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onSuccess(pdfFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onError(e.localizedMessage ?: context.getString(R.string.pdf_error_generate))
            }
        }
    }

    fun resetUpdateState() {
        _uiState.value = _uiState.value.copy(statusUpdateState = Resource.Idle)
    }
}
