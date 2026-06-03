# Red y Almacenamiento Seguro — EPN

**Proyecto Práctico Calificado**  
FIS — Programación de Aplicaciones Móviles  
Escuela Politécnica Nacional

---

## Estructura del proyecto

```
RedSeguridad/
├── gradle/libs.versions.toml          # Catálogo de versiones centralizado
├── build.gradle.kts                   # Build raíz
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts               # Dependencias del módulo
    └── src/main/
        ├── AndroidManifest.xml        # Permiso INTERNET declarado
        └── java/com/epn/redseguridad/
            ├── MainActivity.kt
            ├── data/
            │   ├── remote/
            │   │   ├── Post.kt            # Modelo @Serializable
            │   │   └── PostRepository.kt  # GET + PUT con Ktor
            │   └── storage/
            │       └── SecureStorageRepository.kt  # 3 mecanismos nativos
            ├── viewmodel/
            │   ├── RestViewModel.kt       # Estado UI + loading states
            │   └── SecretsViewModel.kt    # Estado UI + transacciones
            └── ui/
                ├── navigation/
                │   ├── Screen.kt
                │   └── AppNavGraph.kt
                ├── rest/
                │   └── RestScreen.kt      # Módulo 1
                ├── secrets/
                │   └── SecretsScreen.kt   # Módulo 3
                └── theme/
                    └── Theme.kt
```

---

## Cómo abrir en Android Studio

1. Clonar o descomprimir la carpeta `RedSeguridad/`.
2. En Android Studio → **File → Open** → seleccionar la carpeta raíz.
3. Dejar que Gradle sincronice (primera vez descarga ~60MB de dependencias).
4. Seleccionar un emulador API 26+ o dispositivo físico.
5. Presionar **Run ▶**.

> **Nota**: El proyecto requiere conexión a internet en el emulador para que
> el Módulo 1 alcance `jsonplaceholder.typicode.com`.

---

## Módulo 1 — Conectividad REST (30%)

**Tecnología**: Ktor Client (Android engine) + kotlinx.serialization

| Operación | Endpoint | Comportamiento |
|-----------|----------|----------------|
| GET | `/posts/{id}` | Consulta post por ID numérico (1–100). Deshabilita campos durante el tránsito. |
| PUT | `/posts/{id}` | Envía JSON modificado. Captura código 200 OK y actualiza estado visual. |

### Loading states
- `isLoading = true` → TextField + Button deshabilitados (UX reactiva).
- `CircularProgressIndicator` visible dentro del botón durante el tránsito.
- `isLoading = false` → controles restaurados con resultado o error.

---

## Módulo 3 — Almacenamiento Seguro (30%)

**Tecnología**: APIs nativas de Android bajo el principio de conocimiento previo de llave.

| Mecanismo | Encriptación | Acceso | Caso de uso académico |
|-----------|-------------|--------|----------------------|
| `SharedPreferences` | Ninguna (texto plano) | Síncrono / XML | Preferencias de UI, banderas globales |
| `Jetpack DataStore` | Ninguna (texto plano) | Reactivo / Kotlin Flow | Migración moderna, evita bloqueo UI thread |
| `EncryptedSharedPreferences` | AES-256 SIV + AES-128 GCM | Cifrado automático en disco | Tokens JWT, credenciales, llaves privadas |

### Comportamiento transaccional (sin listar claves)
- **Guardar**: usuario ingresa `Llave` + `Valor` + elige mecanismo → `persist`.
- **Recuperar**: usuario ingresa `Llave` + elige mecanismo → revela valor o notifica inexistencia.

---

## Rúbrica cubierta

| Criterio | % | Implementado en |
|----------|---|-----------------|
| Módulo 1 — REST API (GET/PUT con loading states) | 30% | `PostRepository` + `RestViewModel` + `RestScreen` |
| Módulo 3 — Seguridad (SharedPref + DataStore + Encrypted) | 30% | `SecureStorageRepository` + `SecretsViewModel` + `SecretsScreen` |
| Gestión de Estado (reactividad sin caída de procesos) | 20% | `StateFlow` + `collectAsStateWithLifecycle` en ambas pantallas |
| Sustentación — correspondencia framework ↔ Android nativo | 20% | Documentada en `SecureStorageRepository.kt` y `PostRepository.kt` |

---

## Dependencias principales

```toml
ktor = "2.3.12"           # HTTP Client asíncrono
datastore = "1.1.1"       # Jetpack DataStore
securityCrypto = "1.1.0-alpha06"  # EncryptedSharedPreferences
composeBom = "2024.09.03" # Jetpack Compose
navigationCompose = "2.8.2"
```
