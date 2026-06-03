package com.epn.redseguridad.ui.rest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.redseguridad.viewmodel.RestViewModel

/**
 * Pantalla del Módulo 1 — Conectividad REST.
 *
 * Flujo GET:
 *   Usuario ingresa ID → presiona Consultar → se deshabilitan controles (loading)
 *   → se recibe Post → se muestran campos editables.
 *
 * Flujo PUT:
 *   Usuario edita title/body → presiona Actualizar → se deshabilitan controles
 *   → código 200 OK capturado → mensaje de confirmación visual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestScreen(
    onNavigateToSecrets: () -> Unit,
    viewModel: RestViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Módulo 1 · Conectividad REST", fontWeight = FontWeight.Bold)
                        Text(
                            "JSONPlaceholder · GET / PUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSecrets) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Ir a Almacenamiento Seguro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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

            // ── Sección GET ────────────────────────────────────────────────────
            SectionCard(title = "Consulta (GET)") {
                Text(
                    "Ingresa un ID (1 – 100) para obtener el post de JSONPlaceholder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.idInput,
                        onValueChange = viewModel::onIdInputChange,
                        label = { Text("ID del post") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            viewModel.getPost()
                        }),
                        singleLine = true,
                        // Deshabilitar durante el tránsito (loading state)
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.getPost()
                        },
                        enabled = !state.isLoading && state.idInput.isNotBlank()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Consultar")
                        }
                    }
                }
            }

            // ── Sección PUT (visible solo cuando hay un post cargado) ─────────
            AnimatedVisibility(
                visible = state.post != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.post?.let { post ->
                    SectionCard(title = "Actualización (PUT)") {
                        Text(
                            "Post #${post.id} · Usuario ${post.userId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = post.title,
                            onValueChange = viewModel::onTitleChange,
                            label = { Text("Título") },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = post.body,
                            onValueChange = viewModel::onBodyChange,
                            label = { Text("Cuerpo") },
                            enabled = !state.isLoading,
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = viewModel::updatePost,
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Enviando PUT…")
                            } else {
                                Text("Actualizar (PUT /posts/${post.id})")
                            }
                        }
                    }
                }
            }

            // ── Mensajes de feedback ──────────────────────────────────────────
            state.errorMessage?.let { msg ->
                FeedbackCard(message = msg, isError = true, onDismiss = viewModel::clearMessages)
            }
            state.successMessage?.let { msg ->
                FeedbackCard(message = msg, isError = false, onDismiss = viewModel::clearMessages)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun FeedbackCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val containerColor = if (isError)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (isError)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("OK", color = textColor)
            }
        }
    }
}
