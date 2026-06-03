package com.epn.redseguridad.ui.navigation

/**
 * Rutas selladas de la aplicación.
 * NavGraph conecta las dos pantallas del proyecto.
 */
sealed class Screen(val route: String) {
    data object Rest : Screen("rest")
    data object Secrets : Screen("secrets")
}
