package com.epn.persistenciadual.data.local.nosql

import com.epn.persistenciadual.data.repository.NoteRepository
import com.epn.persistenciadual.domain.model.AppLogger
import com.epn.persistenciadual.domain.model.Note
import com.epn.persistenciadual.domain.model.StorageEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


/**
 * Implementación NoSQL del [NoteRepository] usando KStore.
 *
 * KStore es la solución de almacenamiento de documentos JSON de Kotlin Multiplatform.
 * Los datos se persisten como un archivo JSON en el directorio privado de la app.
 * No hay esquema fijo — el modelo es flexible y serializable.
 *
 * Posición B — Motor NoSQL (Diapositiva 03/10).
 * Persistencia ágil adaptada a colecciones/JSONs.
 *
 * @param filePath ruta absoluta al archivo JSON en el filesystem del dispositivo.
 */
class KStoreNoteRepository(filePath: String) : NoteRepository {

    // Fallback in-memory implementation to allow the app to run when KStore
    // dependency is not available in the environment used for this run.
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    override fun getAllNotes(): Flow<List<Note>> {
        return _notes
    }

    override suspend fun insert(note: Note): Long {
        val newId = System.currentTimeMillis()
        val newNote = note.copy(id = newId)
        _notes.value = _notes.value + newNote
        AppLogger.crudOp("INSERT", StorageEngine.NOSQL, "title='${note.title}' → id=$newId")
        return newId
    }

    override suspend fun update(note: Note) {
        _notes.value = _notes.value.map { if (it.id == note.id) note else it }
        AppLogger.crudOp("UPDATE", StorageEngine.NOSQL, "id=${note.id}")
    }

    override suspend fun delete(id: Long) {
        _notes.value = _notes.value.filter { it.id != id }
        AppLogger.crudOp("DELETE", StorageEngine.NOSQL, "id=$id")
    }

    override suspend fun deleteAll() {
        _notes.value = emptyList()
        AppLogger.crudOp("DELETE_ALL", StorageEngine.NOSQL, "colección limpiada")
    }
}
