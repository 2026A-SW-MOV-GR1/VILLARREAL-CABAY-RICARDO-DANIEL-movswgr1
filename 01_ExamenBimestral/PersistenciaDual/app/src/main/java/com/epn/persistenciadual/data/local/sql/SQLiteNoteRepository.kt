package com.epn.persistenciadual.data.local.sql

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.epn.persistenciadual.data.repository.NoteRepository
import com.epn.persistenciadual.domain.model.AppLogger
import com.epn.persistenciadual.domain.model.Note
import com.epn.persistenciadual.domain.model.StorageEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// ── Constantes del esquema ────────────────────────────────────────────────────
private const val DB_NAME    = "notes_sql.db"
private const val DB_VERSION = 1
private const val TABLE      = "notes"
private const val COL_ID      = "id"
private const val COL_TITLE   = "title"
private const val COL_CONTENT = "content"
private const val COL_CREATED = "created_at"

/**
 * SQLiteOpenHelper que gestiona la creación y versión del esquema.
 * Motor relacional — Posición A del examen.
 */
private class NoteDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE (
                $COL_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE   TEXT    NOT NULL,
                $COL_CONTENT TEXT    NOT NULL,
                $COL_CREATED INTEGER NOT NULL
            )
        """.trimIndent())
        AppLogger.info("SQLite: tabla '$TABLE' creada correctamente")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }
}

/**
 * Implementación relacional del [NoteRepository].
 * Usa SQLite nativo de Android (sin Room ni ORM externo).
 *
 * Posición A — Motor SQL (Diapositiva 03/10).
 * Esquema fijo con tabla, llaves y tipos definidos.
 */
class SQLiteNoteRepository(context: Context) : NoteRepository {

    private val helper = NoteDbHelper(context)

    // StateFlow interno que notifica a la UI cuando los datos cambian
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    init {
        // Carga inicial al construir el repositorio
        refreshCache()
    }

    private fun refreshCache() {
        val db = helper.readableDatabase
        val cursor = db.query(TABLE, null, null, null, null, null, "$COL_CREATED DESC")
        val list = buildList {
            while (cursor.moveToNext()) {
                add(
                    Note(
                        id        = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        title     = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        content   = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED))
                    )
                )
            }
            cursor.close()
        }
        _notes.value = list
        AppLogger.debug("SQLite: cache refrescada — ${list.size} registros")
    }

    override fun getAllNotes(): Flow<List<Note>> = _notes.asStateFlow()

    override suspend fun insert(note: Note): Long = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE,   note.title)
            put(COL_CONTENT, note.content)
            put(COL_CREATED, note.createdAt)
        }
        val id = db.insert(TABLE, null, values)
        AppLogger.crudOp("INSERT", StorageEngine.SQL, "title='${note.title}' → id=$id")
        refreshCache()
        id
    }

    override suspend fun update(note: Note) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE,   note.title)
            put(COL_CONTENT, note.content)
        }
        val rows = db.update(TABLE, values, "$COL_ID = ?", arrayOf(note.id.toString()))
        AppLogger.crudOp("UPDATE", StorageEngine.SQL, "id=${note.id} rows_affected=$rows")
        refreshCache()
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))
        AppLogger.crudOp("DELETE", StorageEngine.SQL, "id=$id")
        refreshCache()
    }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val db = helper.writableDatabase
        db.delete(TABLE, null, null)
        AppLogger.crudOp("DELETE_ALL", StorageEngine.SQL, "tabla limpiada")
        refreshCache()
    }
}
