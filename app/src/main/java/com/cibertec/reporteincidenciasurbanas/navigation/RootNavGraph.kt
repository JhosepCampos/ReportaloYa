package com.cibertec.reporteincidenciasurbanas.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlueDark
import com.cibertec.reporteincidenciasurbanas.viewmodel.AuthViewModel

@Composable
fun RootNavGraph(
    context: Context,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        route = NavRoutes.ROOT_GRAPH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                initialCheckDone = authUiState.initialCheckDone,
                onProceed = {
                    val user = authUiState.currentUser
                    if (user != null) {
                        if (user.esAdmin) {
                            navController.navigate(NavRoutes.ADMIN_GRAPH) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavRoutes.CITIZEN_GRAPH) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(NavRoutes.AUTH_GRAPH) {
                            popUpTo(NavRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        authNavGraph(
            navController = navController,
            authViewModel = authViewModel
        )

        citizenNavGraph(
            navController = navController,
            context = context,
            currentUser = authUiState.currentUser,
            onLogout = {
                authViewModel.logout()
                navController.navigate(NavRoutes.AUTH_GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )

        adminNavGraph(
            navController = navController,
            currentUser = authUiState.currentUser,
            onLogout = {
                authViewModel.logout()
                navController.navigate(NavRoutes.AUTH_GRAPH) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun SplashScreen(
    initialCheckDone: Boolean,
    onProceed: () -> Unit
) {
    LaunchedEffect(initialCheckDone) {
        if (initialCheckDone) {
            onProceed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryBlueDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ReportaloYa",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Gestión Comunitaria de Incidencias",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}
