package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.screens.tours.TourPackagesScreen
import com.example.ui.screens.settings.SettingsScreen

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

    if (!isLoggedIn) {
        LoginScreen(
            adminRepository = appContainer.adminRepository,
            // isLoggedIn flipping to true (from sessionManager) already swaps this whole
            // branch out for the Scaffold+NavHost below, whose NavHost starts at
            // Screen.Dashboard.route. Calling navController.navigate() here would crash,
            // because at this point (still in the LoginScreen branch) that NavHost hasn't
            // been composed yet, so its graph isn't set.
            onLoginSuccess = {}
        )
    } else {
        BoxWithConstraints {
            // On phones keep the bottom navigation available on every screen, including
            // submodule screens. On larger screens use a direct header menu instead of
            // a dropdown/bottom navigation.
            val isWideScreen = maxWidth >= 700.dp

            fun navigateFromMainMenu(route: String) {
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

            Scaffold(
                topBar = {
                    if (isWideScreen) {
                        AdminHeaderNavigation(
                            currentRoute = currentRoute,
                            onNavigate = ::navigateFromMainMenu
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(visible = !isWideScreen) {
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
                // Bottom Bar Screens
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

                // Submodule Screens
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
}


@Composable
private fun AdminHeaderNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val menuScreens = listOf(
        Screen.Dashboard,
        Screen.Bookings,
        Screen.Buses,
        Screen.Operators,
        Screen.Routes,
        Screen.Schedules,
        Screen.Counters,
        Screen.Fares,
        Screen.MiniCoaches,
        Screen.TourPackages,
        Screen.Districts,
        Screen.Admins,
        Screen.Settings
    )

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            menuScreens.forEach { screen ->
                val selected = currentRoute == screen.route
                TextButton(
                    onClick = { onNavigate(screen.route) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.icon,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 5.dp)
                    )
                    Text(
                        text = screen.bottomLabel,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
