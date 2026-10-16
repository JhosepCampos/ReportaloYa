package com.cibertec.reporteincidenciasurbanas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.R
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

import android.content.Context
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File

data class CitizenReportsUiState(
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val selectedStatusFilter: String = "TODOS",
    val allIncidents: List<Incident> = emptyList(),
    val filteredIncidents: List<Incident> = emptyList(),
    val errorMessage: String? = null
)

class CitizenReportsViewModel(
    private val incidentRepository: IncidentRepository = IncidentRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenReportsUiState())
    val uiState: StateFlow<CitizenReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            incidentRepository.getCitizenIncidents(uid).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val all = resource.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            allIncidents = all,
                            filteredIncidents = applyFilter(all, _uiState.value.selectedStatusFilter),
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

    fun onFilterSelected(status: String) {
        _uiState.value = _uiState.value.copy(
            selectedStatusFilter = status,
            filteredIncidents = applyFilter(_uiState.value.allIncidents, status)
        )
    }

    fun exportAllReportsPdf(
        context: Context,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val incidents = _uiState.value.allIncidents
        if (incidents.isEmpty()) {
            onError(context.getString(R.string.pdf_empty_list))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingPdf = true)
            try {
                val uid = authRepository.getCurrentUserUid()
                var user: User? = null
                if (uid != null) {
                    val userRes = userRepository.getUserProfile(uid).firstOrNull { it is Resource.Success }
                    if (userRes is Resource.Success) {
                        user = userRes.data
                    }
                }

                val pdfFile = withContext(Dispatchers.IO) {
                    PdfExporter.generateAllIncidentsPdf(context, incidents, user)
                }

                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onSuccess(pdfFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onError(e.localizedMessage ?: context.getString(R.string.pdf_error_generate))
            }
        }
    }

    private fun applyFilter(list: List<Incident>, filter: String): List<Incident> {
        if (filter == "TODOS") return list
        return list.filter { it.estado == filter }
    }
}
