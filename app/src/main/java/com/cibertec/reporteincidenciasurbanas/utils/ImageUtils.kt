package com.cibertec.reporteincidenciasurbanas.utils

import android.util.Base64
import android.util.LruCache

object ImageUtils {

    private val base64Cache = LruCache<String, ByteArray>(50)

    fun getImageModel(fotografiaUrl: String?): Any? {
        if (fotografiaUrl.isNullOrBlank()) return null
        return if (fotografiaUrl.startsWith("data:") || fotografiaUrl.contains("base64,")) {
            val cachedBytes = base64Cache.get(fotografiaUrl)
            if (cachedBytes != null) {
                return cachedBytes
            }
            try {
                val base64String = fotografiaUrl.substringAfter("base64,")
                val decoded = Base64.decode(base64String, Base64.DEFAULT)
                base64Cache.put(fotografiaUrl, decoded)
                decoded
            } catch (e: Exception) {
                fotografiaUrl
            }
        } else {
            fotografiaUrl
        }
    }
}
