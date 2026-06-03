package com.epn.redseguridad.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epn.redseguridad.data.storage.SecureStorageRepository
import com.epn.redseguridad.data.storage.StorageMechanism
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado inmutable de la pantalla de gestión de secretos.
 *
 * @param keyInput          llave ingresada por el usuario
 * @param valueInput        valor ingresado por el usuario
 * @param selectedMechanism mecanismo de almacenamiento activo
 * @param isLoading         operación en tránsito
 * @param retrievedValue    valor recuperado tras una acción Recuperar
 * @param feedbackMessage   mensaje de resultado (éxito o error)
 * @param isSaveMode        true = modo Guardar, false = modo Recuperar
 */
data class SecretsUiState(
    val keyInput: String = "",
    val valueInput: String = "",
    val selectedMechanism: StorageMechanism = StorageMechanism.SHARED_PREFERENCES,
    val isLoading: Boolean = false,
    val retrievedValue: String? = null,
    val feedbackMessage: String? = null,
    val isSaveMode: Boolean = true
)

/**
 * ViewModel del Módulo 3 — Almacenamiento Seguro.
 *
 * Extiende AndroidViewModel para acceder al Context necesario
 * por EncryptedSharedPreferences y DataStore.
 *
 * Comportamiento transaccional (sin listar claves):
 * - Guardar: llave + valor + selector de mecanismo → persist
 * - Recuperar: llave + selector → reveal o "no encontrado"
 */
class SecretsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecureStorageRepository(application)

    private val _uiState = MutableStateFlow(SecretsUiState())
    val uiState: StateFlow<SecretsUiState> = _uiState.asStateFlow()

    fun onKeyChange(value: String) {
        _uiState.update { it.copy(keyInput = value, feedbackMessage = null, retrievedValue = null) }
    }

    fun onValueChange(value: String) {
        _uiState.update { it.copy(valueInput = value, feedbackMessage = null) }
    }

    fun onMechanismSelected(mechanism: StorageMechanism) {
        _uiState.update {
            it.copy(
                selectedMechanism = mechanism,
                feedbackMessage = null,
                retrievedValue = null
            )
        }
    }

    fun onModeToggle(isSave: Boolean) {
        _uiState.update {
            it.copy(isSaveMode = isSave, feedbackMessage = null, retrievedValue = null)
        }
    }

    /**
     * Acción Guardar: persiste [keyInput] → [valueInput] en [selectedMechanism].
     * Valida que ningún campo esté vacío antes de proceder.
     */
    fun saveSecret() {
        val state = _uiState.value
        if (state.keyInput.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "⚠ La llave no puede estar vacía") }
            return
        }
        if (state.valueInput.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "⚠ El valor no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
            runCatching {
                repository.save(state.selectedMechanism, state.keyInput, state.valueInput)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "✓ Guardado en ${state.selectedMechanism.label}",
                        valueInput = ""
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "✗ Error al guardar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Acción Recuperar: busca [keyInput] en [selectedMechanism].
     * Si existe, revela el valor; si no, notifica de forma genérica.
     */
    fun retrieveSecret() {
        val state = _uiState.value
        if (state.keyInput.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "⚠ Ingresa la llave a buscar") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null, retrievedValue = null) }
            runCatching {
                repository.retrieve(state.selectedMechanism, state.keyInput)
            }.onSuccess { value ->
                if (value != null) {
                    _uiState.update {
                        it.copy(isLoading = false, retrievedValue = value)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            feedbackMessage = "ℹ Llave no encontrada en ${state.selectedMechanism.label}"
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "✗ Error al recuperar: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, retrievedValue = null) }
    }
}
