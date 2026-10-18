package com.gymlist.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymlist.app.data.model.Exercise

/**
 * Selector de ejercicios del catálogo. No se cierra al añadir uno, porque lo normal es meter
 * varios de seguido al montar un día de rutina.
 */
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filter by remember { mutableStateOf(ExerciseFilter()) }
    var justAdded by remember { mutableStateOf(emptySet<String>()) }

    val filtered = remember(exercises, filter) { exercises.applyFilter(filter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Añadir ejercicio",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${filtered.size} de ${exercises.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            ExerciseSearchField(
                value = filter.query,
                onValueChange = { filter = filter.copy(query = it) },
                placeholder = "Buscar por nombre, músculo o material"
            )
            Spacer(Modifier.height(10.dp))
            ExerciseFilterChips(filter = filter, onFilterChange = { filter = it })
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Ningún ejercicio coincide con la búsqueda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            items(filtered, key = { it.id }) { exercise ->
                val added = exercise.id in justAdded
                ExerciseRow(exercise) {
                    TextButton(
                        onClick = {
                            onPick(exercise)
                            justAdded = justAdded + exercise.id
                        }
                    ) {
                        Icon(
                            if (added) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (added) "Añadido" else "Añadir")
                    }
                }
            }
        }
    }
}
