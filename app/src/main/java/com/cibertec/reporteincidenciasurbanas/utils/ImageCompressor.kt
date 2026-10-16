package com.cibertec.reporteincidenciasurbanas.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageCompressor {

    suspend fun compressImageFromUri(
        context: Context,
        imageUri: Uri,
        maxSizeKB: Int = 400
    ): ByteArray = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) {
            throw IllegalArgumentException("No se pudo decodificar la imagen seleccionada")
        }

        val maxDimension = 1280
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > maxDimension || height > maxDimension) {
            max(width.toFloat() / maxDimension, height.toFloat() / maxDimension)
        } else {
            1.0f
        }

        val scaledBitmap = if (scale > 1.0f) {
            val targetWidth = (width / scale).toInt()
            val targetHeight = (height / scale).toInt()
            Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } else {
            originalBitmap
        }

        var quality = 85
        var outputBytes: ByteArray
        do {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputBytes = outputStream.toByteArray()
            quality -= 10
        } while (outputBytes.size > maxSizeKB * 1024 && quality > 20)

        outputBytes
    }

    fun bytesToBase64DataUrl(bytes: ByteArray): String {
        val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }
}
