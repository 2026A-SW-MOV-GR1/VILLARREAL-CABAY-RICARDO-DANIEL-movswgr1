package com.epn.redseguridad.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Resultado sellado que encapsula éxito o error de red.
 * Evita que la capa de UI deba manejar excepciones directamente.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
}

/**
 * Repositorio de red: realiza peticiones HTTP asíncronas contra
 * la Fake REST API de JSONPlaceholder (https://jsonplaceholder.typicode.com).
 *
 * Módulo 1 — Conectividad REST
 */
class PostRepository {

    private val BASE_URL = "https://jsonplaceholder.typicode.com"
    private val postCache = ConcurrentHashMap<Int, Post>()

    /** Cliente Ktor configurado con serialización JSON y logging */
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    /**
     * GET /posts/{id}
     * Consulta un post por su identificador único.
     * @return [NetworkResult.Success] con el [Post], o [NetworkResult.Error] con el mensaje.
     */
    suspend fun getPost(id: Int): NetworkResult<Post> {
        return try {
            postCache[id]?.let { cachedPost ->
                return NetworkResult.Success(cachedPost)
            }

            val response = client.get("$BASE_URL/posts/$id")
            if (response.status == HttpStatusCode.OK) {
                val post = response.body<Post>()
                postCache[post.id] = post
                NetworkResult.Success(post)
            } else {
                NetworkResult.Error("Error ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de red: ${e.localizedMessage ?: "desconocido"}")
        }
    }

    /**
     * PUT /posts/{id}
     * Envía el JSON modificado de vuelta al servidor.
     * JSONPlaceholder responde con 200 OK y el recurso actualizado (simulado).
     * @return [NetworkResult.Success] con el [Post] actualizado, o [NetworkResult.Error].
     */
    suspend fun updatePost(post: Post): NetworkResult<Post> {
        return try {
            val response = client.put("$BASE_URL/posts/${post.id}") {
                contentType(ContentType.Application.Json)
                setBody(post)
            }
            if (response.status == HttpStatusCode.OK) {
                val updatedPost = response.body<Post>()
                postCache[updatedPost.id] = updatedPost
                NetworkResult.Success(updatedPost)
            } else {
                NetworkResult.Error("Error ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de red: ${e.localizedMessage ?: "desconocido"}")
        }
    }
}
