package com.epn.redseguridad.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.epn.redseguridad.ui.rest.RestScreen
import com.epn.redseguridad.ui.secrets.SecretsScreen

/**
 * Grafo de navegación de la aplicación.
 * Punto de inicio: pantalla REST (Módulo 1).
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Rest.route
    ) {
        composable(Screen.Rest.route) {
            RestScreen(onNavigateToSecrets = {
                navController.navigate(Screen.Secrets.route)
            })
        }
        composable(Screen.Secrets.route) {
            SecretsScreen(onNavigateBack = {
                navController.popBackStack()
            })
        }
    }
}
