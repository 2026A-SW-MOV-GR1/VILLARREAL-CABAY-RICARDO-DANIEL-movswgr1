package com.epn.redseguridad.ui.secrets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.redseguridad.data.storage.StorageMechanism
import com.epn.redseguridad.viewmodel.SecretsViewModel

/**
 * Pantalla del Módulo 3 — Gestión de Secretos.
 *
 * Comportamiento transaccional directo (sin listar claves):
 *
 * Acción Guardar:
 *   Llave + Valor + Selector de mecanismo → persist en el compartimento nativo.
 *
 * Acción Recuperar:
 *   Llave + Selector → reveal si existe, notificación genérica si no.
 *
 * Los tres mecanismos evaluables:
 *   1. SharedPreferences   — texto plano, síncrono, XML
 *   2. Jetpack DataStore   — texto plano, asíncrono, Flow
 *   3. EncryptedSharedPref — AES-256 SIV + AES-128 GCM, cifrado en disco
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecretsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Módulo 3 · Almacenamiento Seguro", fontWeight = FontWeight.Bold)
                        Text(
                            "SharedPrefs · DataStore · EncryptedSharedPref",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Selector de mecanismo de almacenamiento ───────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Compartimento nativo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Selecciona dónde guardar o recuperar el secreto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    StorageMechanism.entries.forEach { mechanism ->
                        MechanismOption(
                            mechanism = mechanism,
                            isSelected = state.selectedMechanism == mechanism,
                            onSelect = { viewModel.onMechanismSelected(mechanism) }
                        )
                        if (mechanism != StorageMechanism.entries.last()) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            // ── Toggle Guardar / Recuperar ────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Acción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.isSaveMode,
                            onClick = { viewModel.onModeToggle(true) },
                            label = { Text("Guardar") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !state.isSaveMode,
                            onClick = { viewModel.onModeToggle(false) },
                            label = { Text("Recuperar") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Formulario transaccional ──────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (state.isSaveMode) "Guardar secreto" else "Recuperar secreto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Campo Llave (siempre visible)
                    OutlinedTextField(
                        value = state.keyInput,
                        onValueChange = viewModel::onKeyChange,
                        label = { Text("Llave") },
                        placeholder = { Text("ej: api_token") },
                        singleLine = true,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo Valor (solo en modo Guardar)
                    AnimatedVisibility(visible = state.isSaveMode) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.valueInput,
                                onValueChange = viewModel::onValueChange,
                                label = { Text("Valor / Secreto") },
                                placeholder = { Text("ej: eyJhbGciOiJIUzI1NiJ9…") },
                                singleLine = true,
                                enabled = !state.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = if (state.isSaveMode) viewModel::saveSecret else viewModel::retrieveSecret,
                        enabled = !state.isLoading && state.keyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Procesando…")
                        } else {
                            Text(
                                if (state.isSaveMode)
                                    "Guardar en ${state.selectedMechanism.label}"
                                else
                                    "Recuperar de ${state.selectedMechanism.label}"
                            )
                        }
                    }
                }
            }

            // ── Valor recuperado ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.retrievedValue != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.retrievedValue?.let { value ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Secreto recuperado",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Origen: ${state.selectedMechanism.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // ── Feedback general ──────────────────────────────────────────────
            state.feedbackMessage?.let { msg ->
                val isError = msg.startsWith("✗") || msg.startsWith("⚠")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearFeedback) {
                            Text("OK")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MechanismOption(
    mechanism: StorageMechanism,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Column {
                Text(
                    mechanism.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    mechanism.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
