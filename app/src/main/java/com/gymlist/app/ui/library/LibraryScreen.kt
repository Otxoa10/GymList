package com.gymlist.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.repo.GymRepository
import com.gymlist.app.data.repo.Taxonomy
import com.gymlist.app.ui.common.EmptyState
import com.gymlist.app.ui.common.SectionTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(repository: GymRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { LibraryViewModel(repository) }
        }
    }
}

/**
 * El catálogo completo, para mirar la técnica sin estar entrenando. Cada ejercicio abre el mismo
 * diálogo (i) con imágenes, músculos, pasos y consejos.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(ExerciseFilter()) }

    val filtered = remember(exercises, filter) { exercises.applyFilter(filter) }
    // Agrupadas por zona, en el orden en que se entrena normalmente, no alfabético.
    val grouped = remember(filtered) {
        filtered.groupBy { it.bodyPartKey }
            .toList()
            .sortedBy { (key, _) ->
                Taxonomy.bodyPartOrder.indexOf(key).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            ExerciseSearchField(
                value = filter.query,
                onValueChange = { filter = filter.copy(query = it) }
            )
            Spacer(Modifier.height(10.dp))
            ExerciseFilterChips(filter = filter, onFilterChange = { filter = it })
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (filtered.size == 1) "1 ejercicio" else "${filtered.size} ejercicios",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (filter.isActive) {
                    TextButton(onClick = { filter = ExerciseFilter() }) { Text("Quitar filtros") }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "Ningún ejercicio coincide",
                    message = "Prueba con otra palabra o quita alguno de los filtros."
                )
            }
        }

        grouped.forEach { (bodyPartKey, list) ->
            item(key = "head-$bodyPartKey") {
                Spacer(Modifier.height(10.dp))
                SectionTitle("${Taxonomy.bodyPart(bodyPartKey)} · ${list.size}")
            }
            items(list, key = { it.id }) { exercise ->
                ExerciseRow(exercise)
            }
        }
    }
}
