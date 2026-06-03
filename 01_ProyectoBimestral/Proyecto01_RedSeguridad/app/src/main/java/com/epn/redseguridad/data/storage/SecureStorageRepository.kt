package com.epn.redseguridad.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.first

// Extensión de nivel de archivo para crear el DataStore singleton
private val Context.dataStore by preferencesDataStore(name = "epn_datastore")

/**
 * Enum que representa los tres mecanismos de persistencia a evaluar.
 * Módulo 3 — Almacenamiento Seguro (Diapositiva 06/09)
 */
enum class StorageMechanism(val label: String, val description: String) {
    SHARED_PREFERENCES(
        label = "SharedPreferences",
        description = "Texto plano · XML · Síncrono"
    ),
    DATASTORE(
        label = "Jetpack DataStore",
        description = "Texto plano · Flow · Asíncrono"
    ),
    ENCRYPTED_SHARED_PREFERENCES(
        label = "EncryptedSharedPref",
        description = "AES-256 SIV + AES-128 GCM · Cifrado en disco"
    )
}

/**
 * Repositorio que unifica los tres mecanismos de almacenamiento nativos de Android.
 *
 * Principio de conocimiento previo de llave: el usuario debe saber la llave
 * para poder recuperar su secreto — la UI no lista claves existentes.
 *
 * Módulo 3 — Almacenamiento Seguro
 */
class SecureStorageRepository(private val context: Context) {

    // ── 1. SharedPreferences (texto plano, síncrono) ──────────────────────────
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("epn_shared_prefs", Context.MODE_PRIVATE)
    }

    // ── 2. Jetpack DataStore (texto plano, reactivo, asíncrono) ───────────────
    // (context.dataStore se crea a través de la extensión de nivel de archivo)

    // ── 3. EncryptedSharedPreferences (AES-256 SIV + AES-128 GCM) ─────────────
    private val encryptedSharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "epn_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Guarda [value] bajo [key] en el mecanismo especificado por [mechanism].
     * Las operaciones síncromas (SharedPreferences) son seguras aquí porque el
     * ViewModel las despacha desde una corrutina IO.
     */
    suspend fun save(mechanism: StorageMechanism, key: String, value: String) {
        when (mechanism) {
            StorageMechanism.SHARED_PREFERENCES -> {
                sharedPreferences.edit().putString(key, value).apply()
            }
            StorageMechanism.DATASTORE -> {
                val prefKey = stringPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs[prefKey] = value
                }
            }
            StorageMechanism.ENCRYPTED_SHARED_PREFERENCES -> {
                encryptedSharedPreferences.edit().putString(key, value).apply()
            }
        }
    }

    /**
     * Recupera el valor bajo [key] del mecanismo indicado.
     * Retorna null si la llave no existe en ese compartimento.
     */
    suspend fun retrieve(mechanism: StorageMechanism, key: String): String? {
        return when (mechanism) {
            StorageMechanism.SHARED_PREFERENCES -> {
                sharedPreferences.getString(key, null)
            }
            StorageMechanism.DATASTORE -> {
                val prefKey = stringPreferencesKey(key)
                context.dataStore.data.first()[prefKey]
            }
            StorageMechanism.ENCRYPTED_SHARED_PREFERENCES -> {
                encryptedSharedPreferences.getString(key, null)
            }
        }
    }
}
