# Examen: Persistencia Dual — EPN

**Evaluación Práctica de Código**
FIS — Programación de Aplicaciones Móviles | Semestre 2026

---

## Estructura del proyecto

```
PersistenciaDual/
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── app/src/
    ├── main/java/com/epn/persistenciadual/
    │   ├── MainActivity.kt
    │   ├── domain/model/
    │   │   ├── Note.kt              ← Modelo de dominio @Serializable
    │   │   └── StorageEngine.kt     ← Enum + AppLogger estructurado
    │   ├── data/
    │   │   ├── repository/
    │   │   │   └── NoteRepository.kt  ← Interfaz — Patrón Repositorio
    │   │   └── local/
    │   │       ├── sql/
    │   │       │   └── SQLiteNoteRepository.kt   ← Motor A: SQLite nativo
    │   │       └── nosql/
    │   │           └── KStoreNoteRepository.kt   ← Motor B: KStore/JSON
    │   ├── viewmodel/
    │   │   └── NoteViewModel.kt     ← Conmutación en tiempo de ejecución
    │   └── ui/
    │       ├── crud/CrudScreen.kt   ← Switch + Chip + CRUD reactivo
    │       └── theme/Theme.kt
    └── test/java/com/epn/persistenciadual/
        ├── FakeNoteRepository.kt    ← Repositorio en memoria para tests
        └── NoteRepositoryTest.kt    ← 6 pruebas unitarias JVM
```

---

## Cómo abrir y correr

1. **File → Open** → carpeta `PersistenciaDual/`
2. Dejar sincronizar Gradle (~2 min)
3. Emulador API 26+ o dispositivo físico → **▶ Run**

### Correr las pruebas unitarias

```
./gradlew :app:test
```

O en Android Studio → clic derecho sobre `NoteRepositoryTest` → **Run Tests**

---

## Rúbrica cubierta (40%)

| Criterio | % | Implementado en |
|----------|---|-----------------|
| Conmutación correcta SQL ↔ NoSQL con reactividad en UI | 40% | `NoteViewModel.switchEngine()` + `CrudScreen` Switch/Chip |
| Patrón Repositorio + Logs estructurados | 20% | `NoteRepository` interface + `AppLogger` DEBUG/INFO/ERROR |
| Suite de Pruebas Unitarias (6 tests JVM) | 20% | `NoteRepositoryTest.kt` |
| Sustentación oral — diseño y decisiones | 20% | Documentación en cada archivo |

---

## Decisiones de diseño

### ¿Por qué KStore y no Hive/Realm?

El examen sugiere Hive, Isar, Realm como opciones NoSQL para KMP. Se eligió **KStore** porque:
- Es la solución oficial de Kotlin Multiplatform para persistencia de documentos JSON.
- No requiere code generation ni plugins adicionales de Gradle.
- Compatible con el mismo patrón Flow que usa StateFlow en el ViewModel.
- Hive requiere Flutter; Realm tiene licenciamiento más complejo para KMP.

### ¿Cómo funciona la conmutación sin recrear el ViewModel?

```
switchEngine(NoSQL)
  │
  ├── activeRepo = noSqlRepo        ← cambia la referencia
  ├── collectJob?.cancel()          ← cancela suscripción anterior
  └── collectNotes()                ← abre nueva suscripción al Flow de noSQL
                                       → UI se actualiza instantáneamente
```

El ViewModel **nunca se destruye** al conmutar. Solo cambia qué Flow observa.

### ¿Por qué los registros SQL no contaminan NoSQL?

Cada motor tiene su propio almacenamiento físico:
- **SQLite** → `/data/data/com.epn.persistenciadual/databases/notes_sql.db`
- **KStore**  → `/data/data/com.epn.persistenciadual/files/notes_kstore.json`

Son archivos completamente separados. El ViewModel mantiene dos instancias
de repositorio distintas (`sqlRepo`, `noSqlRepo`) que nunca comparten estado.

### Logs estructurados

Todos los cambios de motor e inserciones imprimen en Logcat:
```
[INFO]  Motor conmutado: SQLite → KStore / JSON
[DEBUG] [INSERT] engine=SQLite | title='Mi nota' → id=1
[DEBUG] [DELETE] engine=KStore / JSON | id=1631234567890
[ERROR] deleteNote id=99 ... (si falla)
```

Filtrar en Logcat con tag `PersistenciaDual`.

---

## Dependencias clave

```toml
kstore-file = "io.github.xxfast:kstore-file:0.8.0"   # NoSQL KMP
kotlinx-serialization-json = "1.7.3"                  # Serialización del modelo
kotlinx-coroutines-test = "1.8.1"                     # Tests con runTest
```
