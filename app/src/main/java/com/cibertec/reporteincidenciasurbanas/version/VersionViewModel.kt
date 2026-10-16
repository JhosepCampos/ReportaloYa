package com.cibertec.reporteincidenciasurbanas.version

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class VersionUiState(
    val isLoading: Boolean = false,
    val isUpdateRequired: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercentage: Int = 0,
    val isInstalling: Boolean = false,
    val config: AppVersionConfig? = null,
    val currentVersionCode: Int = 1,
    val currentVersionName: String = "1.0.0",
    val errorMessage: String? = null
)

class VersionViewModel(
    private val versionRepository: VersionRepository = VersionRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionUiState())
    val uiState: StateFlow<VersionUiState> = _uiState.asStateFlow()

    fun checkVersion(context: Context) {
        val (installedVersionCode, installedVersionName) = getInstalledVersion(context)
        _uiState.value = _uiState.value.copy(
            currentVersionCode = installedVersionCode,
            currentVersionName = installedVersionName
        )

        viewModelScope.launch {
            versionRepository.getAppVersionConfig().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Resource.Success -> {
                        val config = resource.data
                        val requiresUpdate = config.forceUpdate && (installedVersionCode < config.minVersionCode)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            config = config,
                            isUpdateRequired = requiresUpdate,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun startUpdate(context: Context) {
        val config = _uiState.value.config
        val downloadUrl = config?.apkDownloadUrl

        if (downloadUrl.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.version_update_error_not_configured)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgressPercentage = 0,
                errorMessage = null
            )

            versionRepository.downloadApk(context, downloadUrl).collect { status ->
                when (status) {
                    is DownloadStatus.Progress -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = true,
                            downloadProgressPercentage = status.percentage
                        )
                    }
                    is DownloadStatus.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            isInstalling = true,
                            downloadProgressPercentage = 100
                        )
                        installApk(context, status.apkFile)
                    }
                    is DownloadStatus.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            errorMessage = context.getString(R.string.version_update_error_download, status.message)
                        )
                    }
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            versionRepository.openApkInstaller(context, apkFile)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = e.localizedMessage ?: "Error al iniciar el instalador de Android"
            )
        }
    }

    private fun getInstalledVersion(context: Context): Pair<Int, String> {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(pInfo).toInt()
            val name = pInfo.versionName ?: "1.0.0"
            Pair(code, name)
        } catch (_: Exception) {
            Pair(1, "1.0.0")
        }
    }
}
