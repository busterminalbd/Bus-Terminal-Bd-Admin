package com.example.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.network.AppUpdateInfo
import com.example.data.network.UpdateChecker
import com.example.di.AppContainer
import com.example.ui.components.UpdateAvailableDialog
import com.example.ui.screens.admins.AdminsScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.bookings.BookingsScreen
import com.example.ui.screens.buses.BusesScreen
import com.example.ui.screens.counters.CountersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.districts.DistrictsScreen
import com.example.ui.screens.fares.FaresScreen
import com.example.ui.screens.minicoaches.MiniCoachesScreen
import com.example.ui.screens.operators.OperatorsScreen
import com.example.ui.screens.routes.RoutesScreen
import com.example.ui.screens.schedules.SchedulesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tours.TourPackagesScreen

@Composable
fun AppNavigation(appContainer: AppContainer) {
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    // One-time check per app launch: is a newer signed APK published on GitHub Releases?
    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.checkForUpdate()
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(info = info, onDismiss = { updateInfo = null })
    }

    val isLoggedIn by appContainer.adminRepository.isLoggedIn.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // On phones/tablets use the bottom navigation on every logged-in screen so
    // submodules opened from Dashboard/Settings never hide the main navigation.
    // On large screens use a direct, always-visible header menu instead.
    val isLargeScreen =
        LocalConfiguration.current.screenWidthDp >= 600

    val navigateFromMainMenu: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            adminRepository = appContainer.adminRepository,
            onLoginSuccess = {}
        )
    } else {
        Scaffold(
            topBar = {
                if (isLargeScreen) {
                    LargeScreenHeader(
                        currentRoute = currentRoute,
                        onNavigate = navigateFromMainMenu
                    )
                }
            },
            bottomBar = {
                if (!isLargeScreen) {
                    NavigationBar {
                        BottomNavScreens.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navigateFromMainMenu(screen.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                                        contentDescription = screen.titleBn
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.bottomLabel,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        dashboardRepository = appContainer.dashboardRepository,
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.Bookings.route) {
                    BookingsScreen(
                        bookingRepository = appContainer.bookingRepository
                    )
                }

                composable(Screen.Buses.route) {
                    BusesScreen(
                        busRepository = appContainer.busRepository,
                        operatorRepository = appContainer.operatorRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader
                    )
                }

                composable(Screen.Counters.route) {
                    CountersScreen(
                        counterRepository = appContainer.counterRepository,
                        busRepository = appContainer.busRepository,
                        districtRepository = appContainer.districtRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        adminRepository = appContainer.adminRepository,
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                        onLogout = {
                            appContainer.adminRepository.logout()
                        }
                    )
                }

                composable(Screen.Operators.route) {
                    OperatorsScreen(
                        operatorRepository = appContainer.operatorRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Routes.route) {
                    RoutesScreen(
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Schedules.route) {
                    SchedulesScreen(
                        scheduleRepository = appContainer.scheduleRepository,
                        busRepository = appContainer.busRepository,
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Fares.route) {
                    FaresScreen(
                        fareRepository = appContainer.fareRepository,
                        busRepository = appContainer.busRepository,
                        routeRepository = appContainer.routeRepository,
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MiniCoaches.route) {
                    MiniCoachesScreen(
                        miniCoachRepository = appContainer.miniCoachRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.TourPackages.route) {
                    TourPackagesScreen(
                        tourPackageRepository = appContainer.tourPackageRepository,
                        cloudinaryUploader = appContainer.cloudinaryUploader,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Districts.route) {
                    DistrictsScreen(
                        districtRepository = appContainer.districtRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Admins.route) {
                    AdminsScreen(
                        adminRepository = appContainer.adminRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LargeScreenHeader(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainMenuScreens.forEach { screen ->
                val selected = currentRoute == screen.route

                TextButton(
                    onClick = { onNavigate(screen.route) },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.icon,
                        contentDescription = screen.titleBn,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = screen.titleBn,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private val MainMenuScreens = listOf(
    Screen.Dashboard,
    Screen.Bookings,
    Screen.Buses,
    Screen.Counters,
    Screen.Settings,
    Screen.Operators,
    Screen.Routes,
    Screen.Schedules,
    Screen.Fares,
    Screen.MiniCoaches,
    Screen.TourPackages,
    Screen.Districts,
    Screen.Admins
)
