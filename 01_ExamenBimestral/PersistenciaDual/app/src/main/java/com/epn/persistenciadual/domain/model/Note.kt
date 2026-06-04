package com.epn.persistenciadual.domain.model

import kotlinx.serialization.Serializable

/**
 * Modelo de dominio central de la aplicación.
 *
 * Este objeto es independiente del motor de persistencia:
 * - SQLite lo almacena en una tabla con columnas tipadas.
 * - KStore (NoSQL) lo serializa como JSON en un archivo local.
 *
 * La anotación @Serializable es necesaria para KStore/JSON.
 * Para SQLite, ContentValues lo mapea manualmente (sin ORM externo).
 */
@Serializable
data class Note(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
