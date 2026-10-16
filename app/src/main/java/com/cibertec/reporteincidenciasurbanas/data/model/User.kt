package com.cibertec.reporteincidenciasurbanas.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class UserRole {
    CIUDADANO,
    ADMINISTRADOR;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "ADMINISTRADOR", "ADMIN" -> ADMINISTRADOR
                else -> CIUDADANO
            }
        }
    }
}

data class User(
    val id: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val rol: String = UserRole.CIUDADANO.name,
    val fechaRegistro: Timestamp? = null,
    val activo: Boolean = true
) {
    @get:Exclude
    val nombreCompleto: String
        get() = "$nombres $apellidos".trim()

    @get:Exclude
    val esAdmin: Boolean
        get() = UserRole.fromString(rol) == UserRole.ADMINISTRADOR
}
