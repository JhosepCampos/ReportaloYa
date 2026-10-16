package com.cibertec.reporteincidenciasurbanas.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.ui.admin.AdminDetailScreen
import com.cibertec.reporteincidenciasurbanas.ui.admin.AdminMainScreen
import com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.IncidentDetailViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ProfileViewModel

fun NavGraphBuilder.adminNavGraph(
    navController: NavController,
    currentUser: User?,
    onLogout: () -> Unit
) {
    navigation(
        startDestination = NavRoutes.ADMIN_DASHBOARD,
        route = NavRoutes.ADMIN_GRAPH
    ) {
        composable(NavRoutes.ADMIN_DASHBOARD) {
            val adminViewModel: AdminDashboardViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            AdminMainScreen(
                currentUser = currentUser,
                adminViewModel = adminViewModel,
                chatViewModel = chatViewModel,
                profileViewModel = profileViewModel,
                onNavigateToDetail = { id -> navController.navigate(NavRoutes.adminDetail(id)) },
                onLogout = onLogout
            )
        }

        composable(
            route = NavRoutes.ADMIN_DETAIL,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getString("incidentId") ?: return@composable
            val detailViewModel: IncidentDetailViewModel = viewModel()
            val adminViewModel: AdminDashboardViewModel = viewModel()

            AdminDetailScreen(
                incidentId = incidentId,
                detailViewModel = detailViewModel,
                adminViewModel = adminViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
