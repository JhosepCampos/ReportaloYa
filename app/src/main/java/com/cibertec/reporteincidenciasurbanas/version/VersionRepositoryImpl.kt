package com.cibertec.reporteincidenciasurbanas.version

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL

class VersionRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VersionRepository {

    companion object {
        private const val COLLECTION_APP_CONFIG = "app_config"
        private const val DOCUMENT_VERSION = "version_control"
    }

    init {
        // Habilitar manejo automático de cookies para descargas con confirmación (Google Drive, etc.)
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        }
    }

    override fun getAppVersionConfig(): Flow<Resource<AppVersionConfig>> = flow {
        emit(Resource.Loading)
        try {
            val docSnapshot = firestore.collection(COLLECTION_APP_CONFIG)
                .document(DOCUMENT_VERSION)
                .get()
                .await()

            if (docSnapshot.exists()) {
                val config = docSnapshot.toObject(AppVersionConfig::class.java) ?: AppVersionConfig()
                emit(Resource.Success(config))
            } else {
                val defaultConfig = AppVersionConfig(
                    minVersionCode = 1,
                    latestVersionCode = 1,
                    latestVersionName = "1.0.0",
                    forceUpdate = false,
                    releaseNotes = "Versión inicial estable."
                )
                emit(Resource.Success(defaultConfig))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al consultar versión en el servidor"))
        }
    }.flowOn(Dispatchers.IO)

    override fun downloadApk(context: Context, downloadUrl: String): Flow<DownloadStatus> = flow {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "update_latest.apk")
        if (apkFile.exists()) {
            apkFile.delete()
        }

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var currentUrl = sanitizeDownloadUrl(downloadUrl)
            var redirects = 0
            val maxRedirects = 10

            // Seguir redirecciones HTTP/HTTPS automáticamente (Google Drive, Dropbox, CDNs)
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 25000
                    readTimeout = 60000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        connection.disconnect()
                        redirects++
                        continue
                    }
                }
                break
            }

            if (connection == null || connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadStatus.Error("Servidor respondió con código: ${connection?.responseCode ?: "Error de red"}"))
                return@flow
            }

            val fileLength = connection.contentLength.toLong()
            inputStream = connection.inputStream
            outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var totalDownloaded: Long = 0
            var bytesRead: Int
            var lastEmittedPercentage = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead

                if (fileLength > 0) {
                    val percentage = ((totalDownloaded * 100) / fileLength).toInt().coerceIn(0, 99)
                    if (percentage != lastEmittedPercentage) {
                        lastEmittedPercentage = percentage
                        emit(DownloadStatus.Progress(percentage, totalDownloaded, fileLength))
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Validar que el archivo descargado sea un paquete APK/ZIP binario real y no una página HTML
            if (apkFile.length() < 1024) {
                apkFile.delete()
                emit(DownloadStatus.Error("El enlace proporcionó un archivo incompleto o corrupto."))
                return@flow
            }

            val header = ByteArray(4)
            FileInputStream(apkFile).use { fis ->
                fis.read(header)
            }

            val isZipSignature = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() // Magic bytes 'PK'
            if (!isZipSignature) {
                apkFile.delete()
                emit(DownloadStatus.Error("El enlace proporcionó una página web o confirmación HTML en lugar del archivo APK directo."))
                return@flow
            }

            emit(DownloadStatus.Success(apkFile))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Error durante la descarga del APK"))
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    override fun openApkInstaller(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(installIntent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Instalar actualización"))
        }
    }

    private fun sanitizeDownloadUrl(rawUrl: String): String {
        var url = rawUrl.trim()

        // 1. Transformar automáticamente enlaces de Google Drive a descarga directa real
        if (url.contains("drive.google.com")) {
            val idRegex = Regex("(?:id=|/d/)([a-zA-Z0-9_-]+)")
            val match = idRegex.find(url)
            if (match != null) {
                val fileId = match.groupValues[1]
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
            }
        }

        // 2. Transformar enlaces de Dropbox a descarga binaria directa
        if (url.contains("dropbox.com") && url.contains("dl=0")) {
            return url.replace("dl=0", "dl=1")
        }

        return url
    }
}
