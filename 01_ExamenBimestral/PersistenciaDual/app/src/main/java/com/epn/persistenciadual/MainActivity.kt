package com.epn.persistenciadual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.epn.persistenciadual.ui.crud.CrudScreen
import com.epn.persistenciadual.ui.theme.PersistenciaDualTheme

/**
 * Examen: Persistencia Dual en Móviles
 * FIS — Programación de Aplicaciones Móviles
 * Escuela Politécnica Nacional — Semestre 2026
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersistenciaDualTheme {
                CrudScreen()
            }
        }
    }
}
