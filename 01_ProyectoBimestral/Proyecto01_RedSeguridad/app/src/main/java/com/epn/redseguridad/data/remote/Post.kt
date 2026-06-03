package com.epn.redseguridad.data.remote

import kotlinx.serialization.Serializable

/**
 * Modelo de datos que representa un Post de JSONPlaceholder.
 * Usado tanto para la respuesta GET como para el cuerpo del PUT.
 */
@Serializable
data class Post(
    val id: Int = 0,
    val userId: Int = 0,
    val title: String = "",
    val body: String = ""
)
