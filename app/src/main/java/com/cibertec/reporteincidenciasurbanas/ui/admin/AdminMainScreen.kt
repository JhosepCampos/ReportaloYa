package com.cibertec.reporteincidenciasurbanas.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.cibertec.reporteincidenciasurbanas.ui.chat.ChatScreen
import com.cibertec.reporteincidenciasurbanas.ui.profile.ProfileScreen
import com.cibertec.reporteincidenciasurbanas.ui.theme.PrimaryBlue
import com.cibertec.reporteincidenciasurbanas.ui.theme.ReporteIncidenciasUrbanasTheme
import com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ChatViewModel
import com.cibertec.reporteincidenciasurbanas.viewmodel.ProfileViewModel

sealed class AdminBottomNavItem(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false
) {
    object Home : AdminBottomNavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Reports : AdminBottomNavItem(R.string.nav_reports_admin, Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object Chat : AdminBottomNavItem(R.string.nav_chat, Icons.Filled.Chat, Icons.Outlined.ChatBubbleOutline, hasBadge = true)
    object Profile : AdminBottomNavItem(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun AdminMainScreen(
    currentUser: User?,
    adminViewModel: AdminDashboardViewModel,
    chatViewModel: ChatViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val unreadChatCount by chatViewModel.unreadCount.collectAsState()

    AdminMainContent(
        selectedItemIndex = selectedItemIndex,
        unreadChatCount = unreadChatCount,
        onItemSelected = { selectedItemIndex = it },
        content = { index ->
            when (index) {
                0 -> AdminHomeScreen(
                    currentUser = currentUser,
                    viewModel = adminViewModel,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToAllReports = { selectedItemIndex = 1 }
                )
                1 -> AdminDashboardScreen(
                    viewModel = adminViewModel,
                    onNavigateToDetail = onNavigateToDetail
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
fun AdminMainContent(
    selectedItemIndex: Int,
    unreadChatCount: Int = 0,
    onItemSelected: (Int) -> Unit,
    content: @Composable (Int) -> Unit
) {
    val items = listOf(
        AdminBottomNavItem.Home,
        AdminBottomNavItem.Reports,
        AdminBottomNavItem.Chat,
        AdminBottomNavItem.Profile
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
fun AdminMainScreenPreview() {
    ReporteIncidenciasUrbanasTheme {
        AdminMainContent(
            selectedItemIndex = 0,
            unreadChatCount = 3,
            onItemSelected = {},
            content = {
                AdminHomeContent(
                    currentUser = User(nombres = "Admin", apellidos = "Municipal", rol = UserRole.ADMINISTRADOR.name),
                    uiState = com.cibertec.reporteincidenciasurbanas.viewmodel.AdminDashboardUiState(
                        totalIncidents = 12,
                        pendingCount = 4,
                        inProgressCount = 5,
                        resolvedCount = 3
                    ),
                    onNavigateToDetail = {},
                    onNavigateToAllReports = {}
                )
            }
        )
    }
}
