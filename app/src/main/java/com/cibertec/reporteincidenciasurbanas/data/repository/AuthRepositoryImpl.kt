package com.cibertec.reporteincidenciasurbanas.data.repository

import android.content.Context
import com.cibertec.reporteincidenciasurbanas.IncidenciasApp
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val context: Context = IncidenciasApp.instance,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override fun login(email: String, pass: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("No se pudo obtener el identificador de usuario")

            val doc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)?.copy(id = uid)
                    ?: User(id = uid, correo = email, rol = UserRole.CIUDADANO.name)
                emit(Resource.Success(user))
            } else {
                val newUser = User(
                    id = uid,
                    nombres = authResult.user?.displayName ?: "Usuario",
                    apellidos = "",
                    correo = email,
                    rol = UserRole.CIUDADANO.name,
                    fechaRegistro = Timestamp.now(),
                    activo = true
                )
                firestore.collection(Constants.COLLECTION_USERS).document(uid).set(newUser).await()
                emit(Resource.Success(newUser))
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("password", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_wrong_password)
                e.message?.contains("user", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_user_not_found)
                e.message?.contains("network", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_network)
                else ->
                    context.getString(
                        R.string.auth_error_login_generic,
                        e.localizedMessage ?: context.getString(R.string.auth_error_verify_credentials)
                    )
            }
            emit(Resource.Error(message, e))
        }
    }.flowOn(Dispatchers.IO)

    override fun register(
        nombres: String,
        apellidos: String,
        email: String,
        pass: String
    ): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al crear cuenta en el servidor")

            val newUser = User(
                id = uid,
                nombres = nombres.trim(),
                apellidos = apellidos.trim(),
                correo = email.trim(),
                rol = UserRole.CIUDADANO.name,
                fechaRegistro = Timestamp.now(),
                activo = true
            )

            firestore.collection(Constants.COLLECTION_USERS).document(uid).set(newUser).await()
            emit(Resource.Success(newUser))
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("already in use", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_email_already_in_use)
                e.message?.contains("weak-password", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_weak_password)
                else ->
                    context.getString(
                        R.string.auth_error_register_generic,
                        e.localizedMessage ?: context.getString(R.string.auth_error_try_again)
                    )
            }
            emit(Resource.Error(message, e))
        }
    }.flowOn(Dispatchers.IO)

    override fun sendPasswordReset(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val cleanEmail = email.trim()

            // Validar que el correo exista en la base de datos de usuarios
            var userQuery = firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("correo", cleanEmail)
                .limit(1)
                .get()
                .await()

            if (userQuery.isEmpty && cleanEmail != cleanEmail.lowercase()) {
                userQuery = firestore.collection(Constants.COLLECTION_USERS)
                    .whereEqualTo("correo", cleanEmail.lowercase())
                    .limit(1)
                    .get()
                    .await()
            }

            if (userQuery.isEmpty) {
                emit(Resource.Error(context.getString(R.string.forgot_password_error_not_registered)))
                return@flow
            }

            auth.sendPasswordResetEmail(cleanEmail).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    context.getString(R.string.auth_error_network)
                e.message?.contains("user-not-found", ignoreCase = true) == true ->
                    context.getString(R.string.forgot_password_error_not_registered)
                else ->
                    context.getString(
                        R.string.forgot_password_error_send_failed,
                        e.localizedMessage ?: context.getString(R.string.auth_error_try_again)
                    )
            }
            emit(Resource.Error(message, e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCurrentUserUid(): String? = auth.currentUser?.uid

    override fun isUserLoggedIn(): Boolean = auth.currentUser != null

    override fun logout() {
        auth.signOut()
    }
}
