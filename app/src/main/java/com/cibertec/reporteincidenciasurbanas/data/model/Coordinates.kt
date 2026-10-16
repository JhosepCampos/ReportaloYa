package com.cibertec.reporteincidenciasurbanas.data.model

data class Coordinates(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val accuracy: Float = 0.0f
) {
    val isValid: Boolean
        get() = latitude != 0.0 && longitude != 0.0
}
