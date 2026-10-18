package com.gymlist.app.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.gymlist.app.ui.NoInsets
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlist.app.data.model.RoutineItem
import com.gymlist.app.data.model.RoutineItemDetail
import com.gymlist.app.ui.common.EmptyState
import com.gymlist.app.ui.common.ExerciseInfoButton
import com.gymlist.app.ui.library.ExercisePickerSheet
import com.gymlist.app.ui.common.SectionTitle
import com.gymlist.app.util.formatRest
import com.gymlist.app.util.formatRestLabel

/**
 * El diseñador de rutinas: añadir ejercicios, ordenarlos y ajustar series, repeticiones y
 * descanso de cada uno.
 */
@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel,
    onBack: () -> Unit,
    onOpenWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RoutineItemDetail?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        detail?.routine?.name ?: "Rutina",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Renombrar") },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar rutina") },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                },
                windowInsets = NoInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (detail == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (!state.loading) Text("Esta rutina ya no existe")
            }
            return@Scaffold
        }

        val items = detail.orderedItems

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NotesField(
                    value = detail.routine.notes,
                    onValueChange = viewModel::updateNotes
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Ejercicios (${items.size})", Modifier.weight(1f))
                    OutlinedButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Añadir")
                    }
                }
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "Rutina vacía",
                        message = "Añade los ejercicios que quieras hacer este día. Para cada uno podrás elegir cuántas series, cuántas repeticiones y cuánto descanso.",
                        action = {
                            Button(onClick = { showPicker = true }) { Text("Añadir ejercicio") }
                        }
                    )
                }
            }

            itemsIndexed(items, key = { _, entry -> entry.item.id }) { index, entry ->
                RoutineItemRow(
                    index = index + 1,
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == items.lastIndex,
                    onEdit = { editing = entry },
                    onDelete = { viewModel.deleteItem(entry.item) },
                    onMoveUp = { viewModel.move(entry.item.id, -1) },
                    onMoveDown = { viewModel.move(entry.item.id, 1) }
                )
            }

            if (items.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (state.workoutInProgress) onOpenWorkout()
                            else viewModel.startWorkout(onOpenWorkout)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.workoutInProgress) "Ir al entrenamiento en curso"
                            else "Empezar este entrenamiento"
                        )
                    }
                    if (state.workoutInProgress) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ya tienes un entrenamiento sin terminar. Finalízalo o descártalo para empezar otro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = state.catalog,
            onPick = { viewModel.addExercise(it.id) },
            onDismiss = { showPicker = false }
        )
    }

    editing?.let { entry ->
        RoutineItemEditorDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.updateItem(updated)
                editing = null
            }
        )
    }

    if (showRenameDialog && detail != null) {
        NameDialog(
            title = "Renombrar rutina",
            label = "Nombre",
            initial = detail.routine.name,
            confirmText = "Guardar",
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                viewModel.rename(it)
                showRenameDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Borrar la rutina?") },
            text = { Text("Los entrenamientos que ya has hecho se mantienen en el calendario.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteRoutine(onBack)
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Las notas se guardan con cada pulsación, pero el texto que se muestra es el local: así el
 * cursor no salta cuando vuelve el valor desde la base de datos.
 */
@Composable
private fun NotesField(value: String, onValueChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (text.isEmpty() && value.isNotEmpty()) text = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text("Notas de la rutina (opcional)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RoutineItemRow(
    index: Int,
    entry: RoutineItemDetail,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$index", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entry.item.sets} × ${entry.item.reps} · ${formatRestLabel(entry.item.restSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExerciseInfoButton(exercise = entry.exercise)
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Subir",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Bajar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar series y descanso",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Quitar de la rutina",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Editor de un ejercicio dentro de la rutina: series, repeticiones y descanso. */
@Composable
private fun RoutineItemEditorDialog(
    entry: RoutineItemDetail,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit
) {
    var sets by remember { mutableStateOf(entry.item.sets) }
    var reps by remember { mutableStateOf(entry.item.reps) }
    var rest by remember { mutableStateOf(entry.item.restSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.exercise.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Stepper(
                    label = "Series",
                    value = "$sets",
                    onMinus = { if (sets > 1) sets-- },
                    onPlus = { if (sets < 12) sets++ }
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.take(16) },
                    label = { Text("Repeticiones") },
                    placeholder = { Text("8-12") },
                    supportingText = { Text("Puedes poner un rango (8-12), un número (10) o tiempo (30 s)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Stepper(
                    label = "Descanso",
                    value = formatRest(rest),
                    onMinus = { rest = (rest - 15).coerceAtLeast(0) },
                    onPlus = { rest = (rest + 15).coerceAtMost(600) }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 60, 90, 120).forEach { preset ->
                        TextButton(onClick = { rest = preset }) { Text(formatRest(preset)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        entry.item.copy(
                            sets = sets,
                            reps = reps.trim().ifEmpty { "8-12" },
                            restSeconds = rest
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onMinus,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(width = 44.dp, height = 40.dp)
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Box(
            Modifier.width(78.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onPlus,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(width = 44.dp, height = 40.dp)
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
