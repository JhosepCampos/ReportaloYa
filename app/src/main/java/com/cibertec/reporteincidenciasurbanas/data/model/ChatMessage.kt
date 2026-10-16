package com.cibertec.reporteincidenciasurbanas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class ChatMessage(
    val id: String = "",
    val remitenteId: String = "",
    val remitenteNombre: String = "",
    val remitenteRol: String = UserRole.CIUDADANO.name,
    val texto: String = "",
    val timestamp: Timestamp? = null,
    val editado: Boolean = false,
    val eliminado: Boolean = false,
    val reacciones: Map<String, String> = emptyMap(),
    val leidoPor: List<String> = emptyList(),

    val reporteCompartidoId: String? = null,
    val reporteCodigo: String? = null,
    val reporteCategoria: String? = null,
    val reporteDescripcion: String? = null,
    val reporteUbicacion: String? = null,
    val reporteLatitud: Double = 0.0,
    val reporteLongitud: Double = 0.0,
    val reporteEstado: String? = null,
    val reporteFotoUrl: String? = null,
    val reporteFechaCreacion: Timestamp? = null,
    val reporteFechaActualizacion: Timestamp? = null
) {
    @get:Exclude
    val esAdmin: Boolean
        get() = UserRole.fromString(remitenteRol) == UserRole.ADMINISTRADOR

    @get:Exclude
    val esReporteCompartido: Boolean
        get() = !reporteCompartidoId.isNullOrBlank()

    fun countReactions(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        reacciones.values.forEach { emoji ->
            counts[emoji] = (counts[emoji] ?: 0) + 1
        }
        return counts
    }
}
