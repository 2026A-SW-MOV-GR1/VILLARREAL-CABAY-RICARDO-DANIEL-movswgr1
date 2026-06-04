package com.epn.persistenciadual.data.repository

import com.epn.persistenciadual.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción común del repositorio de notas.
 *
 * Patrón Repositorio (Diapositiva 05/10):
 * "Las vistas no deben llamar directamente al motor SQL o NoSQL.
 *  Implementa una abstracción común para aislar la interfaz gráfica."
 *
 * La UI y el ViewModel SOLO conocen esta interfaz.
 * SQLiteNoteRepository y KStoreNoteRepository la implementan de forma independiente.
 * El ViewModel recibe la implementación activa por inyección (sin recrearse).
 */
interface NoteRepository {

    /** Flujo reactivo de todas las notas. La UI se actualiza automáticamente. */
    fun getAllNotes(): Flow<List<Note>>

    /** Inserta una nueva nota. Retorna el ID generado. */
    suspend fun insert(note: Note): Long

    /** Actualiza una nota existente (por id). */
    suspend fun update(note: Note)

    /** Elimina una nota por su id. */
    suspend fun delete(id: Long)

    /** Limpia TODOS los registros del motor actual (útil para tests y demos). */
    suspend fun deleteAll()
}
