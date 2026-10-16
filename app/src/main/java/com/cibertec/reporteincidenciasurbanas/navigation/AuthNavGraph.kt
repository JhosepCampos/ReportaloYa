package com.cibertec.reporteincidenciasurbanas.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.ui.auth.ForgotPasswordScreen
import com.cibertec.reporteincidenciasurbanas.ui.auth.LoginScreen
import com.cibertec.reporteincidenciasurbanas.ui.auth.RegisterScreen
import com.cibertec.reporteincidenciasurbanas.ui.auth.WelcomeScreen
import com.cibertec.reporteincidenciasurbanas.viewmodel.AuthViewModel

fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    navigation(
        startDestination = NavRoutes.WELCOME,
        route = NavRoutes.AUTH_GRAPH
    ) {
        composable(NavRoutes.WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(NavRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = { user ->
                    if (user.esAdmin) {
                        navController.navigate(NavRoutes.ADMIN_GRAPH) {
                            popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(NavRoutes.CITIZEN_GRAPH) {
                            popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.CITIZEN_GRAPH) {
                        popUpTo(NavRoutes.AUTH_GRAPH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
