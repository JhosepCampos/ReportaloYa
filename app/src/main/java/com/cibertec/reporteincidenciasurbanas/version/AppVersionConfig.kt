package com.cibertec.reporteincidenciasurbanas.version

import com.google.firebase.Timestamp

data class AppVersionConfig(
    val minVersionCode: Int = 1,
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val apkDownloadUrl: String = "",
    val releaseNotes: String = "",
    val fechaActualizacion: Timestamp? = null
)
