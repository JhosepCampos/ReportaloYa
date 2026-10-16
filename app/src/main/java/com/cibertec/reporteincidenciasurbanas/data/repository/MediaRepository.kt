package com.cibertec.reporteincidenciasurbanas.data.repository

import android.net.Uri
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    suspend fun compressImage(imageUri: Uri): ByteArray
    fun uploadIncidentPhoto(imageBytes: ByteArray, categoryId: String, userId: String): Flow<Resource<String>>
}
