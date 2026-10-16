package com.cibertec.reporteincidenciasurbanas.data.repository

import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override fun getUserProfile(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val doc = firestore.collection(Constants.COLLECTION_USERS).document(userId).get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)?.copy(id = userId)
                    ?: throw Exception("No se pudo mapear el perfil de usuario")
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Perfil de usuario no encontrado"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al cargar perfil: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun checkUserRole(userId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val doc = firestore.collection(Constants.COLLECTION_USERS).document(userId).get().await()
            val role = doc.getString("rol") ?: UserRole.CIUDADANO.name
            emit(Resource.Success(role))
        } catch (e: Exception) {
            emit(Resource.Error("Error al consultar permisos: ${e.localizedMessage}", e))
        }
    }.flowOn(Dispatchers.IO)
}
