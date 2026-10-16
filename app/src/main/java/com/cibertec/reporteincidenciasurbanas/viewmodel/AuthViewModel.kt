package com.cibertec.reporteincidenciasurbanas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.AuthRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepository
import com.cibertec.reporteincidenciasurbanas.data.repository.UserRepositoryImpl
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: User? = null,
    val loginState: Resource<User> = Resource.Idle,
    val registerState: Resource<User> = Resource.Idle,
    val resetPasswordState: Resource<Unit> = Resource.Idle,
    val initialCheckDone: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserUid()
            if (uid != null) {
                userRepository.getUserProfile(uid).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                currentUser = resource.data,
                                initialCheckDone = true
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                currentUser = null,
                                initialCheckDone = true
                            )
                        }
                        else -> {}
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    currentUser = null,
                    initialCheckDone = true
                )
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authRepository.login(email, pass).collect { resource ->
                _uiState.value = _uiState.value.copy(
                    loginState = resource,
                    currentUser = if (resource is Resource.Success) resource.data else _uiState.value.currentUser
                )
            }
        }
    }

    fun register(nombres: String, apellidos: String, email: String, pass: String) {
        viewModelScope.launch {
            authRepository.register(nombres, apellidos, email, pass).collect { resource ->
                _uiState.value = _uiState.value.copy(
                    registerState = resource,
                    currentUser = if (resource is Resource.Success) resource.data else _uiState.value.currentUser
                )
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            authRepository.sendPasswordReset(email).collect { resource ->
                _uiState.value = _uiState.value.copy(resetPasswordState = resource)
            }
        }
    }

    fun resetStates() {
        _uiState.value = _uiState.value.copy(
            loginState = Resource.Idle,
            registerState = Resource.Idle,
            resetPasswordState = Resource.Idle
        )
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState(initialCheckDone = true)
    }
}
