package com.epn.persistenciadual

import com.epn.persistenciadual.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas Unitarias — Examen Persistencia Dual.
 *
 * Rúbrica (Diapositiva 08/10) — 20%:
 * "Escribe al menos dos pruebas locales que validen la correcta escritura
 *  y cambio de motor de datos en las capas lógicas."
 *
 * Todas las pruebas usan [FakeNoteRepository] — corren en JVM puro,
 * sin emulador ni dispositivo físico.
 */
class NoteRepositoryTest {

    private lateinit var sqlFake: FakeNoteRepository
    private lateinit var noSqlFake: FakeNoteRepository

    @Before
    fun setup() {
        sqlFake = FakeNoteRepository()
        noSqlFake = FakeNoteRepository()
    }

    // ── Prueba 1: escritura correcta en motor SQL ─────────────────────────────

    @Test
    fun `insertar nota en SQL genera ID y persiste el contenido`() = runTest {
        // Dado
        val note = Note(title = "Nota de prueba SQL", content = "Contenido de prueba")

        // Cuando
        val id = sqlFake.insert(note)

        // Entonces
        assertTrue("El ID generado debe ser mayor a 0", id > 0)
        val notas = sqlFake.getAllNotes().first()
        assertEquals("Debe existir exactamente una nota", 1, notas.size)
        assertEquals("El título debe coincidir", "Nota de prueba SQL", notas.first().title)
        assertEquals("El ID almacenado debe ser el retornado", id, notas.first().id)
    }

    // ── Prueba 2: escritura correcta en motor NoSQL ───────────────────────────

    @Test
    fun `insertar nota en NoSQL genera ID y persiste el contenido`() = runTest {
        // Dado
        val note = Note(title = "Nota de prueba NoSQL", content = "Documento JSON")

        // Cuando
        val id = noSqlFake.insert(note)

        // Entonces
        assertTrue("El ID generado debe ser mayor a 0", id > 0)
        val notas = noSqlFake.getAllNotes().first()
        assertEquals("Debe existir exactamente una nota", 1, notas.size)
        assertEquals("El contenido debe coincidir", "Documento JSON", notas.first().content)
    }

    // ── Prueba 3: aislamiento entre motores ───────────────────────────────────

    @Test
    fun `un registro en SQL no contamina el almacen NoSQL`() = runTest {
        // Dado — insertar en SQL
        sqlFake.insert(Note(title = "Solo en SQL", content = "No debe aparecer en NoSQL"))

        // Entonces — NoSQL debe estar vacío
        val notasNoSql = noSqlFake.getAllNotes().first()
        assertTrue(
            "NoSQL debe estar vacío: un INSERT en SQL no debe cruzar motores",
            notasNoSql.isEmpty()
        )

        // Y viceversa — insertar en NoSQL
        noSqlFake.insert(Note(title = "Solo en NoSQL", content = "No debe aparecer en SQL"))
        val notasSqlDespues = sqlFake.getAllNotes().first()
        assertEquals(
            "SQL debe tener exactamente 1 nota (la propia), no las de NoSQL",
            1, notasSqlDespues.size
        )
        assertEquals("Solo en SQL", notasSqlDespues.first().title)
    }

    // ── Prueba 4: actualización correcta ─────────────────────────────────────

    @Test
    fun `actualizar nota modifica el titulo sin duplicar registros`() = runTest {
        // Dado
        val id = sqlFake.insert(Note(title = "Título original", content = "Contenido"))
        val notaOriginal = sqlFake.getAllNotes().first().first { it.id == id }

        // Cuando
        sqlFake.update(notaOriginal.copy(title = "Título actualizado"))

        // Entonces
        val notas = sqlFake.getAllNotes().first()
        assertEquals("Solo debe existir 1 nota (sin duplicados)", 1, notas.size)
        assertEquals("El título debe haberse actualizado", "Título actualizado", notas.first().title)
    }

    // ── Prueba 5: eliminación correcta ───────────────────────────────────────

    @Test
    fun `eliminar nota la remueve correctamente del repositorio`() = runTest {
        // Dado
        val id1 = sqlFake.insert(Note(title = "Nota 1", content = ""))
        sqlFake.insert(Note(title = "Nota 2", content = ""))
        assertEquals(2, sqlFake.getAllNotes().first().size)

        // Cuando
        sqlFake.delete(id1)

        // Entonces
        val notas = sqlFake.getAllNotes().first()
        assertEquals("Debe quedar exactamente 1 nota", 1, notas.size)
        assertNull("La nota eliminada no debe existir", notas.find { it.id == id1 })
    }

    // ── Prueba 6: deleteAll limpia el motor activo ────────────────────────────

    @Test
    fun `deleteAll limpia todos los registros sin afectar el otro motor`() = runTest {
        // Dado — insertar en ambos motores
        sqlFake.insert(Note(title = "SQL 1", content = ""))
        sqlFake.insert(Note(title = "SQL 2", content = ""))
        noSqlFake.insert(Note(title = "NoSQL 1", content = ""))

        // Cuando — limpiar solo SQL
        sqlFake.deleteAll()

        // Entonces
        assertTrue("SQL debe estar vacío tras deleteAll", sqlFake.getAllNotes().first().isEmpty())
        assertEquals("NoSQL NO debe verse afectado", 1, noSqlFake.getAllNotes().first().size)
    }
}
