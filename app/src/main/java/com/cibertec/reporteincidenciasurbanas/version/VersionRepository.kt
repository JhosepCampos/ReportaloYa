package com.cibertec.reporteincidenciasurbanas.version

import android.content.Context
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

sealed class DownloadStatus {
    data class Progress(val percentage: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadStatus()
    data class Success(val apkFile: File) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

interface VersionRepository {
    fun getAppVersionConfig(): Flow<Resource<AppVersionConfig>>
    fun downloadApk(context: Context, downloadUrl: String): Flow<DownloadStatus>
    fun openApkInstaller(context: Context, apkFile: File)
}
