package com.cibertec.reporteincidenciasurbanas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class IncidentStatus(val displayName: String) {
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    EN_ATENCION("En atención"),
    RESUELTO("Resuelto");

    companion object {
        fun fromString(status: String?): IncidentStatus {
            return when (status?.uppercase()) {
                "EN_REVISION", "EN REVISION", "EN REVISIÓN" -> EN_REVISION
                "EN_ATENCION", "EN ATENCION", "EN ATENCIÓN" -> EN_ATENCION
                "RESUELTO" -> RESUELTO
                else -> PENDIENTE
            }
        }
    }
}

data class StatusHistoryEntry(
    val estado: String = IncidentStatus.PENDIENTE.name,
    val fecha: Timestamp? = null,
    val cambiadoPor: String = "",
    val nombreAdmin: String = "",
    val observacion: String = ""
)

data class Incident(
    val id: String = "",
    val codigo: String = "",
    val usuarioId: String = "",
    val usuarioNombre: String = "",
    val categoriaId: String = "",
    val categoriaNombre: String = "",
    val descripcion: String = "",
    val fotografiaUrl: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccionReferencial: String = "",
    val estado: String = IncidentStatus.PENDIENTE.name,
    val fechaCreacion: Timestamp? = null,
    val fechaActualizacion: Timestamp? = null,
    val administradorId: String? = null,
    val historialEstados: List<StatusHistoryEntry> = emptyList()
) {
    @get:Exclude
    val incidentStatus: IncidentStatus
        get() = IncidentStatus.fromString(estado)
}
