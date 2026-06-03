package com.epn.redseguridad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epn.redseguridad.data.remote.NetworkResult
import com.epn.redseguridad.data.remote.Post
import com.epn.redseguridad.data.remote.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla REST.
 * Encapsula todo lo que la UI necesita para renderizarse.
 *
 * @param isLoading     true mientras hay una petición en vuelo
 * @param post          post obtenido del servidor (null si no se ha consultado aún)
 * @param errorMessage  mensaje de error a mostrar, null si no hay error
 * @param successMessage mensaje de éxito tras un PUT exitoso
 * @param idInput       valor actual del campo de texto con el ID
 */
data class RestUiState(
    val isLoading: Boolean = false,
    val post: Post? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val idInput: String = ""
)

/**
 * ViewModel del Módulo 1 — Conectividad REST.
 *
 * Responsabilidades:
 * - Realizar peticiones GET y PUT de forma asíncrona
 * - Exponer [RestUiState] como StateFlow para que la UI reaccione
 * - Deshabilitar controles mientras la petición está en tránsito (loading states)
 */
class RestViewModel : ViewModel() {

    private val repository = PostRepository()

    private val _uiState = MutableStateFlow(RestUiState())
    val uiState: StateFlow<RestUiState> = _uiState.asStateFlow()

    /** Actualiza el campo de ID mientras el usuario escribe */
    fun onIdInputChange(value: String) {
        // Solo permite dígitos y máximo 3 caracteres (JSONPlaceholder tiene 100 posts)
        if (value.all { it.isDigit() } && value.length <= 3) {
            _uiState.update { it.copy(idInput = value, errorMessage = null) }
        }
    }

    /**
     * Dispara una petición GET /posts/{id}.
     * Deshabilita campos y botones durante el tránsito (isLoading = true).
     */
    fun getPost() {
        val id = _uiState.value.idInput.toIntOrNull()
        if (id == null || id !in 1..100) {
            _uiState.update { it.copy(errorMessage = "Ingresa un ID entre 1 y 100") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }
            when (val result = repository.getPost(id)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, post = result.data)
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Actualiza el título del post cargado en memoria */
    fun onTitleChange(newTitle: String) {
        _uiState.update { state ->
            state.copy(post = state.post?.copy(title = newTitle))
        }
    }

    /** Actualiza el cuerpo del post cargado en memoria */
    fun onBodyChange(newBody: String) {
        _uiState.update { state ->
            state.copy(post = state.post?.copy(body = newBody))
        }
    }

    /**
     * Dispara una petición PUT /posts/{id} con el JSON modificado.
     * Captura el código 200 OK y actualiza el estado visual.
     */
    fun updatePost() {
        val post = _uiState.value.post ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }
            when (val result = repository.updatePost(post)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        post = result.data,
                        successMessage = "✓ Post actualizado correctamente (200 OK)"
                    )
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Limpia mensajes de estado */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
