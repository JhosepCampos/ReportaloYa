package com.cibertec.reporteincidenciasurbanas.utils

sealed interface Resource<out T> {
    object Idle : Resource<Nothing>
    object Loading : Resource<Nothing>
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>
}
