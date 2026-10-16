package com.cibertec.reporteincidenciasurbanas.data.repository

import android.content.Context
import android.net.Uri
import com.cibertec.reporteincidenciasurbanas.utils.Constants
import com.cibertec.reporteincidenciasurbanas.utils.ImageCompressor
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediaRepositoryImpl(
    private val context: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : MediaRepository {

    override suspend fun compressImage(imageUri: Uri): ByteArray {
        return ImageCompressor.compressImageFromUri(context, imageUri)
    }

    override fun uploadIncidentPhoto(
        imageBytes: ByteArray,
        categoryId: String,
        userId: String
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val timestamp = System.currentTimeMillis()
            val randomId = UUID.randomUUID().toString().take(8)
            val path = "${Constants.STORAGE_INCIDENTS_PATH}/$categoryId/${userId}_${timestamp}_$randomId.jpg"

            val storageRef = storage.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uploadedBy", userId)
                .build()

            storageRef.putBytes(imageBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            emit(Resource.Success(downloadUrl))
        } catch (e: Exception) {
            try {
                val base64DataUrl = ImageCompressor.bytesToBase64DataUrl(imageBytes)
                emit(Resource.Success(base64DataUrl))
            } catch (fallbackEx: Exception) {
                emit(Resource.Error("Error al procesar fotografía: ${fallbackEx.localizedMessage}", fallbackEx))
            }
        }
    }.flowOn(Dispatchers.IO)
}
