package com.cibertec.reporteincidenciasurbanas.navigation

import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.ui.citizen.CitizenMainScreen
import com.cibertec.reporteincidenciasurbanas.ui.citizen.CreateReportScreen
import com.cibertec.reporteincidenciasurbanas.ui.citizen.IncidentDetailScreen
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenReportsViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.CreateReportViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.IncidentDetailViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ProfileViewModel

fun NavGraphBuilder.citizenNavGraph(
    navController: NavController,
    context: Context,
    currentUser: User?,
    onLogout: () -> Unit
) {
    navigation(
        startDestination = NavRoutes.CITIZEN_MAIN,
        route = NavRoutes.CITIZEN_GRAPH
    ) {
        composable(NavRoutes.CITIZEN_MAIN) {
            val homeViewModel: CitizenHomeViewModel = viewModel()
            val reportsViewModel: CitizenReportsViewModel = viewModel()
            val chatViewModel: ChatViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()

            CitizenMainScreen(
                currentUser = currentUser,
                homeViewModel = homeViewModel,
                reportsViewModel = reportsViewModel,
                chatViewModel = chatViewModel,
                profileViewModel = profileViewModel,
                onNavigateToCreateReport = { navController.navigate(NavRoutes.CREATE_REPORT) },
                onNavigateToIncidentDetail = { id -> navController.navigate(NavRoutes.citizenDetail(id)) },
                onLogout = onLogout
            )
        }

        composable(NavRoutes.CREATE_REPORT) {
            val createViewModel: CreateReportViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return CreateReportViewModel(context = context.applicationContext) as T
                    }
                }
            )

            CreateReportScreen(
                viewModel = createViewModel,
                onNavigateBack = { navController.popBackStack() },
                onReportSubmittedSuccess = { incidentId ->
                    navController.navigate(NavRoutes.citizenDetail(incidentId)) {
                        popUpTo(NavRoutes.CREATE_REPORT) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.CITIZEN_DETAIL,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getString("incidentId") ?: return@composable
            val detailViewModel: IncidentDetailViewModel = viewModel()

            IncidentDetailScreen(
                incidentId = incidentId,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
