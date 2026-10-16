package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun login(email: String, pass: String): Flow<Resource<User>>
    fun register(nombres: String, apellidos: String, email: String, pass: String): Flow<Resource<User>>
    fun sendPasswordReset(email: String): Flow<Resource<Unit>>
    fun getCurrentUserUid(): String?
    fun isUserLoggedIn(): Boolean
    fun logout()
}
