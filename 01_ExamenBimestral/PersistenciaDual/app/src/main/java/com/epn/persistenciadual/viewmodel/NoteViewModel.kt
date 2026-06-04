package com.epn.persistenciadual.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epn.persistenciadual.data.local.nosql.KStoreNoteRepository
import com.epn.persistenciadual.data.local.sql.SQLiteNoteRepository
import com.epn.persistenciadual.data.repository.NoteRepository
import com.epn.persistenciadual.domain.model.AppLogger
import com.epn.persistenciadual.domain.model.Note
import com.epn.persistenciadual.domain.model.StorageEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Estado inmutable de la pantalla CRUD.
 *
 * @param notes          lista de notas del motor activo
 * @param activeEngine   motor de persistencia activo (SQL o NoSQL)
 * @param isLoading      operación en curso
 * @param editingNote    nota que está siendo editada (null = modo creación)
 * @param titleInput     campo título del formulario
 * @param contentInput   campo contenido del formulario
 * @param showForm       si el formulario de creación/edición está visible
 * @param feedbackMsg    mensaje de feedback reciente
 */
data class CrudUiState(
    val notes: List<Note> = emptyList(),
    val activeEngine: StorageEngine = StorageEngine.SQL,
    val isLoading: Boolean = false,
    val editingNote: Note? = null,
    val titleInput: String = "",
    val contentInput: String = "",
    val showForm: Boolean = false,
    val feedbackMsg: String? = null
)

/**
 * ViewModel del examen — Persistencia Dual.
 *
 * Responsabilidades clave:
 * 1. Mantener una referencia al [NoteRepository] activo (SQL o NoSQL).
 * 2. Al conmutar el Switch, reemplazar el repositorio SIN recrear el ViewModel
 *    (la UI reacciona reactivamente al cambio de la lista).
 * 3. Exponer [CrudUiState] como StateFlow para la UI Compose.
 * 4. Garantizar que un registro en SQL no contamine el almacén NoSQL y viceversa.
 */
class NoteViewModel(application: Application) : AndroidViewModel(application) {

    // ── Repositorios ──────────────────────────────────────────────────────────
    private val sqlRepo: NoteRepository = SQLiteNoteRepository(application)
    private val noSqlRepo: NoteRepository = KStoreNoteRepository(
        filePath = File(application.filesDir, "notes_kstore.json").absolutePath
    )

    private var activeRepo: NoteRepository = sqlRepo

    // ── Estado UI ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(CrudUiState())
    val uiState: StateFlow<CrudUiState> = _uiState.asStateFlow()

    // Job para cancelar la colección anterior antes de suscribirse a la nueva
    private var collectJob: Job? = null

    init {
        collectNotes()
        AppLogger.info("NoteViewModel inicializado con motor ${StorageEngine.SQL.label}")
    }

    /**
     * Suscribe el StateFlow de notas al repositorio activo.
     * Al conmutar, cancela la suscripción anterior y abre una nueva.
     */
    private fun collectNotes() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            activeRepo.getAllNotes().collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }
    }

    /**
     * Conmuta el motor de persistencia en tiempo de ejecución.
     * La lista de la UI se actualiza al instante sin reiniciar la app.
     * Un registro guardado en SQL no aparece en NoSQL y viceversa.
     */
    fun switchEngine(newEngine: StorageEngine) {
        val current = _uiState.value.activeEngine
        if (current == newEngine) return

        AppLogger.engineSwitch(from = current, to = newEngine)

        activeRepo = when (newEngine) {
            StorageEngine.SQL   -> sqlRepo
            StorageEngine.NOSQL -> noSqlRepo
        }
        _uiState.update {
            it.copy(
                activeEngine = newEngine,
                showForm = false,
                editingNote = null,
                feedbackMsg = "Motor cambiado a ${newEngine.label}"
            )
        }
        collectNotes()
    }

    // ── Formulario ────────────────────────────────────────────────────────────

    fun onTitleChange(v: String)   = _uiState.update { it.copy(titleInput = v) }
    fun onContentChange(v: String) = _uiState.update { it.copy(contentInput = v) }

    fun openCreateForm() {
        _uiState.update { it.copy(showForm = true, editingNote = null, titleInput = "", contentInput = "") }
    }

    fun openEditForm(note: Note) {
        _uiState.update { it.copy(showForm = true, editingNote = note, titleInput = note.title, contentInput = note.content) }
    }

    fun closeForm() {
        _uiState.update { it.copy(showForm = false, editingNote = null, titleInput = "", contentInput = "") }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveNote() {
        val state = _uiState.value
        if (state.titleInput.isBlank()) {
            _uiState.update { it.copy(feedbackMsg = "⚠ El título no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                if (state.editingNote == null) {
                    // Crear
                    val id = activeRepo.insert(
                        Note(title = state.titleInput.trim(), content = state.contentInput.trim())
                    )
                    _uiState.update { it.copy(feedbackMsg = "✓ Nota creada (id=$id)") }
                } else {
                    // Actualizar
                    activeRepo.update(
                        state.editingNote.copy(
                            title = state.titleInput.trim(),
                            content = state.contentInput.trim()
                        )
                    )
                    _uiState.update { it.copy(feedbackMsg = "✓ Nota actualizada") }
                }
            }.onFailure { e ->
                AppLogger.error("saveNote falló", e)
                _uiState.update { it.copy(feedbackMsg = "✗ Error: ${e.localizedMessage}") }
            }
            _uiState.update { it.copy(isLoading = false, showForm = false, editingNote = null) }
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            runCatching { activeRepo.delete(id) }
                .onSuccess { _uiState.update { it.copy(feedbackMsg = "Nota eliminada") } }
                .onFailure { e -> AppLogger.error("deleteNote id=$id", e) }
        }
    }

    fun clearFeedback() = _uiState.update { it.copy(feedbackMsg = null) }

    // ── Utilidad para tests ───────────────────────────────────────────────────

    /** Expone los repositorios internos para pruebas unitarias. */
    fun getSqlRepo(): NoteRepository   = sqlRepo
    fun getNoSqlRepo(): NoteRepository = noSqlRepo
    fun getActiveRepo(): NoteRepository = activeRepo
}
