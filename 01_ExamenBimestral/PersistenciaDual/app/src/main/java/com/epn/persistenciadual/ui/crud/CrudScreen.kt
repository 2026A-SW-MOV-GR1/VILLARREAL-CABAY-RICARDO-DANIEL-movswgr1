package com.epn.persistenciadual.ui.crud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.persistenciadual.domain.model.Note
import com.epn.persistenciadual.domain.model.StorageEngine
import com.epn.persistenciadual.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla CRUD principal del examen Persistencia Dual.
 *
 * Requisitos de UI cubiertos (Diapositiva 04/10):
 * 1. Control Switch en AppBar — conmuta entre SQL y NoSQL en tiempo real.
 * 2. Reactividad instantánea — la lista se actualiza sin reiniciar la app.
 * 3. Indicador de Origen Activo — Chip de color que muestra el motor activo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrudScreen(viewModel: NoteViewModel = viewModel()) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Colores del chip según motor
    val engineColor = when (state.activeEngine) {
        StorageEngine.SQL   -> Color(0xFF1565C0)
        StorageEngine.NOSQL -> Color(0xFF2E7D32)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Persistencia Dual", fontWeight = FontWeight.Bold)
                        Text(
                            "Examen — FIS EPN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // ── Indicador de Origen Activo (Chip) ─────────────────────
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                state.activeEngine.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = engineColor
                        ),
                        border = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // ── Control Switch en AppBar ───────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "SQL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.activeEngine == StorageEngine.SQL)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = state.activeEngine == StorageEngine.NOSQL,
                            onCheckedChange = { isNoSql ->
                                viewModel.switchEngine(
                                    if (isNoSql) StorageEngine.NOSQL else StorageEngine.SQL
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                        Text(
                            "NoSQL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.activeEngine == StorageEngine.NOSQL)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreateForm) {
                Icon(Icons.Default.Add, contentDescription = "Nueva nota")
            }
        }
    ) { innerPadding ->

        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // ── Lista de notas ─────────────────────────────────────────────────
            if (state.notes.isEmpty() && !state.isLoading) {
                EmptyState(engine = state.activeEngine)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { viewModel.openEditForm(note) },
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }
            }

            // ── Formulario crear/editar ────────────────────────────────────────
            AnimatedVisibility(
                visible = state.showForm,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteForm(
                        title = state.titleInput,
                        content = state.contentInput,
                        isEditing = state.editingNote != null,
                        isLoading = state.isLoading,
                        engineLabel = state.activeEngine.label,
                        onTitleChange = viewModel::onTitleChange,
                        onContentChange = viewModel::onContentChange,
                        onSave = viewModel::saveNote,
                        onCancel = viewModel::closeForm
                    )
                }
            }

            // ── Snackbar de feedback ───────────────────────────────────────────
            state.feedbackMsg?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearFeedback) { Text("OK") }
                    }
                ) { Text(msg) }
            }
        }
    }
}

// ── Componentes ────────────────────────────────────────────────────────────────

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note.content.isNotBlank()) {
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    fmt.format(Date(note.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteForm(
    title: String,
    content: String,
    isEditing: Boolean,
    isLoading: Boolean,
    engineLabel: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (isEditing) "Editar nota — $engineLabel" else "Nueva nota — $engineLabel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Título") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            label = { Text("Contenido") },
            enabled = !isLoading,
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(
                onClick = onSave,
                enabled = !isLoading && title.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEditing) "Actualizar" else "Guardar")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(engine: StorageEngine) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sin notas en ${engine.label}", style = MaterialTheme.typography.titleSmall)
            Text(
                "Presiona + para agregar una nota",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
