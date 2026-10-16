package com.cibertec.reporteincidenciasurbanas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cibertec.reporteincidenciasurbanas.navigation.RootNavGraph
import com.cibertec.reporteincidenciasurbanas.ui.components.NoInternetDialog
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.utils.NetworkConnectivityObserver
import com.cibertec.reporteincidenciasurbanas.utils.PermissionUtils
import com.cibertec.reporteincidenciasurbanas.version.VersionUpdateDialog
import com.cibertec.reporteincidenciasurbanas.version.VersionViewModel

class MainActivity : ComponentActivity() {

    private lateinit var connectivityObserver: NetworkConnectivityObserver

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        connectivityObserver = NetworkConnectivityObserver(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        com.cibertec.reporteincidenciasurbanas.services.ChatSyncService.start(this)

        setContent {
            ReporteIncidenciasUrbanasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isConnected by connectivityObserver.isConnected.collectAsState()
                    val versionViewModel: VersionViewModel = viewModel()
                    val versionUiState by versionViewModel.uiState.collectAsState()

                    LaunchedEffect(Unit) {
                        versionViewModel.checkVersion(this@MainActivity)
                    }

                    // Diálogo bloqueante cuando no hay conexión a internet
                    NoInternetDialog(
                        isOpen = !isConnected,
                        onRetry = {
                            connectivityObserver.checkCurrentConnection()
                        },
                        onExit = {
                            finishAffinity()
                        }
                    )

                    VersionUpdateDialog(
                        uiState = versionUiState,
                        onUpdateClick = { versionViewModel.startUpdate(this@MainActivity) },
                        onRetryClick = { versionViewModel.startUpdate(this@MainActivity) }
                    )

                    RootNavGraph(context = this@MainActivity)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityObserver.isInitialized) {
            connectivityObserver.unregister()
        }
    }
}