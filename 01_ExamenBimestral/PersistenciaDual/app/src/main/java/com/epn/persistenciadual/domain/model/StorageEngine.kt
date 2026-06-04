package com.epn.persistenciadual.domain.model

import android.util.Log

/**
 * Enum que representa el motor de persistencia activo.
 * El Switch en la AppBar conmuta entre estos dos valores en tiempo de ejecución.
 */
enum class StorageEngine(val label: String, val chipColor: Long) {
    SQL(label = "SQLite", chipColor = 0xFF1565C0),
    NOSQL(label = "KStore / JSON", chipColor = 0xFF2E7D32)
}

/**
 * Logger estructurado para auditoría de cambios de motor y operaciones CRUD.
 *
 * Principio de ingeniería exigido (Diapositiva 05/10):
 * Cada cambio de base de datos o inserción imprime trazas con tipo DEBUG, INFO o ERROR.
 */
object AppLogger {
    private const val TAG = "PersistenciaDual"

    fun debug(msg: String) {
        Log.d(TAG, "[DEBUG] $msg")
    }

    fun info(msg: String) {
        Log.i(TAG, "[INFO]  $msg")
    }

    fun error(msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[ERROR] $msg", throwable)
        } else {
            Log.e(TAG, "[ERROR] $msg")
        }
    }

    fun engineSwitch(from: StorageEngine, to: StorageEngine) {
        info("Motor conmutado: ${from.label} → ${to.label}")
    }

    fun crudOp(op: String, engine: StorageEngine, detail: String) {
        debug("[$op] engine=${engine.label} | $detail")
    }
}
