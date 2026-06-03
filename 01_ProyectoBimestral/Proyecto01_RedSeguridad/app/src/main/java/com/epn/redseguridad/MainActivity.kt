package com.epn.redseguridad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.epn.redseguridad.ui.navigation.AppNavGraph
import com.epn.redseguridad.ui.theme.RedSeguridadTheme

/**
 * Punto de entrada de la aplicación.
 *
 * Proyecto: Red y Almacenamiento Seguro
 * FIS — Programación de Aplicaciones Móviles
 * Escuela Politécnica Nacional
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedSeguridadTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
