package com.prismspace.container.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prismspace.container.ui.screens.DetailScreen
import com.prismspace.container.ui.screens.EngineDiagnosticsScreen
import com.prismspace.container.ui.screens.PrivacyShieldScreen
import com.prismspace.container.ui.screens.SettingsScreen
import com.prismspace.container.ui.screens.WorkspaceScreen
import com.prismspace.container.ui.theme.PrismSpaceTheme
import com.prismspace.container.ui.viewmodel.PrismSpaceViewModel

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{itemTitle}") {
        fun createRoute(itemTitle: String) = "detail/$itemTitle"
    }
    object Settings : Screen("settings")
    object PrivacyShield : Screen("privacy_shield")
    object EngineDiagnostics : Screen("engine_diagnostics")
}

/**
 * Main navigation graph for Prism Space
 */
@Composable
fun PrismSpaceNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: PrismSpaceViewModel = PrismSpaceViewModel()
) {
    PrismSpaceNavGraphContent(
        navController = navController,
        viewModel = viewModel
    )
}

@Composable
private fun PrismSpaceNavGraphContent(
    navController: NavHostController,
    viewModel: PrismSpaceViewModel? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            WorkspaceScreen(
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                androidx.navigation.navArgument("itemTitle") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val itemTitle = backStackEntry.arguments?.getString("itemTitle") ?: "Item"
            DetailScreen(
                itemTitle = itemTitle,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.Settings.route) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    onBackClick = {
                        navController.navigateUp()
                    },
                    onOpenPrivacyShield = {
                        navController.navigate(Screen.PrivacyShield.route)
                    },
                    viewModel = viewModel
                )

                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.EngineDiagnostics.route) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    },
                    text = { Text("Engine") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                )
            }
        }

        composable(Screen.PrivacyShield.route) {
            PrivacyShieldScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        composable(Screen.EngineDiagnostics.route) {
            EngineDiagnosticsScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PrismSpaceNavGraphPreview() {
    PrismSpaceTheme {
        val navController = rememberNavController()
        PrismSpaceNavGraphContent(navController = navController)
    }
}
