package com.cibertec.reporteincidenciasurbanas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.IncidentStatus
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CitizenHomeUiState(
    val isLoading: Boolean = false,
    val totalReports: Int = 0,
    val inProgressReports: Int = 0,
    val resolvedReports: Int = 0,
    val recentIncidents: List<Incident> = emptyList(),
    val errorMessage: String? = null
)

class CitizenHomeViewModel(
    private val incidentRepository: IncidentRepository = IncidentRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenHomeUiState())
    val uiState: StateFlow<CitizenHomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            incidentRepository.getCitizenIncidents(uid).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val incidents = resource.data
                        val total = incidents.size
                        val inProgress = incidents.count {
                            it.estado == IncidentStatus.EN_REVISION.name || it.estado == IncidentStatus.EN_ATENCION.name
                        }
                        val resolved = incidents.count { it.estado == IncidentStatus.RESUELTO.name }

                        _uiState.value = CitizenHomeUiState(
                            isLoading = false,
                            totalReports = total,
                            inProgressReports = inProgress,
                            resolvedReports = resolved,
                            recentIncidents = incidents.take(3),
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
}
