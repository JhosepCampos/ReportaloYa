package com.cibertec.reporteincidenciasurbanas.ui.citizen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cibertec.reporteincidenciasurbanas.R
import com.cibertec.reporteincidenciasurbanas.data.model.User
import com.cibertec.reporteincidenciasurbanas.data.model.UserRole
import com.cibertec.reporteincidenciasurbanas.ui.profile.ProfileScreen
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenReportsViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ProfileViewModel

import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.collectAsState
import com.cibertec.reporteincidenciasurbanas.ui.chat.ChatScreen
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatViewModel

sealed class BottomNavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false
) {
    object Home : BottomNavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Reports : BottomNavItem(R.string.nav_reports, Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object Chat : BottomNavItem(R.string.nav_chat, Icons.Filled.Chat, Icons.Outlined.ChatBubbleOutline, hasBadge = true)
    object Profile : BottomNavItem(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun CitizenMainScreen(
    currentUser: User?,
    homeViewModel: CitizenHomeViewModel,
    reportsViewModel: CitizenReportsViewModel,
    chatViewModel: ChatViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToIncidentDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val unreadChatCount by chatViewModel.unreadCount.collectAsState()

    CitizenMainContent(
        selectedItemIndex = selectedItemIndex,
        unreadChatCount = unreadChatCount,
        onItemSelected = { selectedItemIndex = it },
        content = { index ->
            when (index) {
                0 -> CitizenHomeScreen(
                    currentUser = currentUser,
                    viewModel = homeViewModel,
                    onNavigateToCreateReport = onNavigateToCreateReport,
                    onNavigateToIncidentDetail = onNavigateToIncidentDetail,
                    onNavigateToAllReports = { selectedItemIndex = 1 }
                )
                1 -> CitizenReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateToDetail = onNavigateToIncidentDetail,
                    onNavigateToCreateReport = onNavigateToCreateReport
                )
                2 -> ChatScreen(
                    viewModel = chatViewModel
                )
                3 -> ProfileScreen(
                    viewModel = profileViewModel,
                    onLogout = onLogout
                )
            }
        }
    )
}

@Composable
fun CitizenMainContent(
    selectedItemIndex: Int,
    unreadChatCount: Int = 0,
    onItemSelected: (Int) -> Unit,
    content: @Composable (Int) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Reports,
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedItemIndex == index
                    val title = stringResource(item.titleRes)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onItemSelected(index) },
                        label = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            if (item.hasBadge && unreadChatCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (unreadChatCount > 99) "99+" else "$unreadChatCount",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = title
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.12f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content(selectedItemIndex)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CitizenMainScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        CitizenMainContent(
            selectedItemIndex = 0,
            onItemSelected = {},
            content = {
                CitizenHomeContent(
                    currentUser = User(
                        id = "1",
                        nombres = "Jhosep",
                        apellidos = "Campos",
                        correo = "jhosep@ejemplo.com",
                        rol = UserRole.CIUDADANO.name
                    ),
                    uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.CitizenHomeUiState(
                        totalReports = 5,
                        inProgressReports = 2,
                        resolvedReports = 3
                    ),
                    onNavigateToCreateReport = {},
                    onNavigateToIncidentDetail = {},
                    onNavigateToAllReports = {}
                )
            }
        )
    }
}
