package com.epn.persistenciadual

import com.epn.persistenciadual.data.repository.NoteRepository
import com.epn.persistenciadual.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación en memoria de [NoteRepository] para pruebas unitarias.
 * No requiere Context, SQLite ni sistema de archivos — es 100% en RAM.
 *
 * Permite validar la lógica de negocio (CRUD, conmutación) de forma
 * aislada, sin dependencias de Android.
 */
class FakeNoteRepository : NoteRepository {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun getAllNotes(): Flow<List<Note>> = _notes.asStateFlow()

    override suspend fun insert(note: Note): Long {
        val id = nextId++
        val newNote = note.copy(id = id)
        _notes.value = _notes.value + newNote
        return id
    }

    override suspend fun update(note: Note) {
        _notes.value = _notes.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun delete(id: Long) {
        _notes.value = _notes.value.filter { it.id != id }
    }

    override suspend fun deleteAll() {
        _notes.value = emptyList()
    }

    /** Helper para inspeccionar el estado actual en los tests */
    fun currentNotes(): List<Note> = _notes.value
}
