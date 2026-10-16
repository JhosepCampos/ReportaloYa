package com.cibertec.reporteincidenciasurbanas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.data.model.ChatMessage
import com.cibertec.reporteincidenciasurbanas.data.model.Incident
import com.cibertec.reporteincidenciasurbanas.data.model.StatusHistoryEntry
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.ChatRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.IncidentRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import android.content.Context
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.utils.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import com.cibertec.reporteincidenciasurbanas.R

data class IncidentDetailUiState(
    val isLoading: Boolean = false,
    val incident: Incident? = null,
    val errorMessage: String? = null,
    val isSharing: Boolean = false,
    val isExportingPdf: Boolean = false
)

class IncidentDetailViewModel(
    private val incidentRepository: IncidentRepository = IncidentRepositoryImpl(),
    private val chatRepository: ChatRepository = ChatRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentDetailUiState())
    val uiState: StateFlow<IncidentDetailUiState> = _uiState.asStateFlow()

    fun loadIncident(incidentId: String) {
        viewModelScope.launch {
            incidentRepository.getIncidentById(incidentId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            incident = resource.data,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            incident = null,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun shareIncidentToCommunityChat(
        context: Context? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val incident = _uiState.value.incident ?: run {
            onError(context?.getString(R.string.pdf_error_load_share) ?: "No se pudo cargar el reporte a compartir")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true)
            val uid = authRepository.getCurrentUserUid() ?: ""
            val defaultAuthorName = context?.getString(R.string.notification_reactor_fallback) ?: "Vecino"
            var authorName = defaultAuthorName
            var authorRole = UserRole.CIUDADANO.name

            if (uid.isNotBlank()) {
                val userRes = userRepository.getUserProfile(uid).firstOrNull { it is Resource.Success }
                if (userRes is Resource.Success && userRes.data != null) {
                    val u = userRes.data
                    authorName = u.nombreCompleto.ifBlank { u.correo }
                    authorRole = u.rol
                }
            }

            val fechaEstadoActual = incident.historialEstados.lastOrNull()?.fecha
                ?: incident.fechaActualizacion
                ?: incident.fechaCreacion

            val shareMessageText = context?.getString(
                R.string.share_to_chat_msg_format,
                incident.categoriaNombre,
                incident.codigo
            ) ?: "📢 Ficha informativa compartida: ${incident.categoriaNombre} (${incident.codigo})"

            val chatMessage = ChatMessage(
                remitenteId = uid,
                remitenteNombre = authorName,
                remitenteRol = authorRole,
                texto = shareMessageText,
                reporteCompartidoId = incident.id,
                reporteCodigo = incident.codigo,
                reporteCategoria = incident.categoriaNombre,
                reporteDescripcion = incident.descripcion,
                reporteUbicacion = incident.direccionReferencial,
                reporteLatitud = incident.latitud,
                reporteLongitud = incident.longitud,
                reporteEstado = incident.estado,
                reporteFotoUrl = incident.fotografiaUrl,
                reporteFechaCreacion = incident.fechaCreacion,
                reporteFechaActualizacion = fechaEstadoActual
            )

            chatRepository.sendMessage(chatMessage).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isSharing = false)
                        onSuccess()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isSharing = false)
                        onError(resource.message ?: (context?.getString(R.string.share_to_chat_error) ?: "Error al compartir en el chat"))
                    }
                    else -> {}
                }
            }
        }
    }

    fun exportIncidentPdf(
        context: Context,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val incident = _uiState.value.incident ?: run {
            onError(context.getString(R.string.pdf_error_load_single))
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
                    PdfExporter.generateSingleIncidentPdf(context, incident, user)
                }

                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onSuccess(pdfFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExportingPdf = false)
                onError(e.localizedMessage ?: context.getString(R.string.pdf_error_generate))
            }
        }
    }

    fun updateStatusOptimistic(newStatus: String, observation: String?, adminName: String): Incident? {
        val currentIncident = _uiState.value.incident ?: return null
        val previousIncident = currentIncident.copy()

        val newHistoryEntry = StatusHistoryEntry(
            estado = newStatus,
            fecha = Timestamp.now(),
            cambiadoPor = adminName,
            nombreAdmin = adminName,
            observacion = observation ?: ""
        )

        val updatedIncident = currentIncident.copy(
            estado = newStatus,
            historialEstados = currentIncident.historialEstados + newHistoryEntry
        )

        _uiState.value = _uiState.value.copy(
            incident = updatedIncident,
            errorMessage = null
        )
        return previousIncident
    }

    fun revertStatus(previousIncident: Incident, errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            incident = previousIncident,
            errorMessage = errorMessage
        )
    }
}
