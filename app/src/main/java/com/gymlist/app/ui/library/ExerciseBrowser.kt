package com.gymlist.app.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gymlist.app.R
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.repo.Taxonomy
import com.gymlist.app.ui.common.ExerciseInfoButton
import com.gymlist.app.util.ExerciseImage
import com.gymlist.app.util.rememberExerciseImages

/**
 * Filtros del catálogo. Con 643 ejercicios, buscar por texto no basta: lo normal es querer ver
 * "lo de pecho", o "lo de principiante que puedo hacer sin material".
 */
data class ExerciseFilter(
    val query: String = "",
    /** Clave de zona del cuerpo (`Taxonomy.bodyParts`), o null para todas. */
    val bodyPart: String? = null,
    /** Clave de dificultad (`Taxonomy.difficulties`), o null para todas. */
    val difficulty: String? = null,
    val bodyweightOnly: Boolean = false
) {
    val isActive: Boolean
        get() = query.isNotBlank() || bodyPart != null || difficulty != null || bodyweightOnly
}

fun List<Exercise>.applyFilter(filter: ExerciseFilter): List<Exercise> {
    val needle = filter.query.normalizeForSearch()
    return filter {
        (filter.bodyPart == null || it.bodyPartKey == filter.bodyPart) &&
            (filter.difficulty == null || it.difficulty == Taxonomy.difficulty(filter.difficulty)) &&
            (!filter.bodyweightOnly || it.isBodyweight) &&
            (needle.isBlank() || it.matches(needle))
    }
}

private fun Exercise.matches(needle: String): Boolean =
    name.normalizeForSearch().contains(needle) ||
        muscleGroup.normalizeForSearch().contains(needle) ||
        equipment.normalizeForSearch().contains(needle) ||
        primaryMuscles.any { it.normalizeForSearch().contains(needle) }

/** Búsqueda sin tildes ni mayúsculas: "jalon" encuentra "Jalón al Pecho". */
internal fun String.normalizeForSearch(): String =
    lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
        .replace('ñ', 'n')
        .trim()

@Composable
fun ExerciseSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar ejercicio"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

/** Dos filas de chips: zona del cuerpo arriba, dificultad y material abajo. */
@Composable
fun ExerciseFilterChips(
    filter: ExerciseFilter,
    onFilterChange: (ExerciseFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.bodyPart == null,
                onClick = { onFilterChange(filter.copy(bodyPart = null)) },
                label = { Text("Todo") }
            )
            Taxonomy.bodyPartOrder.forEach { key ->
                FilterChip(
                    selected = filter.bodyPart == key,
                    onClick = {
                        onFilterChange(filter.copy(bodyPart = if (filter.bodyPart == key) null else key))
                    },
                    label = { Text(Taxonomy.bodyPart(key)) }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("beginner", "intermediate", "advanced").forEach { key ->
                FilterChip(
                    selected = filter.difficulty == key,
                    onClick = {
                        onFilterChange(
                            filter.copy(difficulty = if (filter.difficulty == key) null else key)
                        )
                    },
                    label = { Text(Taxonomy.difficulty(key)) }
                )
            }
            FilterChip(
                selected = filter.bodyweightOnly,
                onClick = { onFilterChange(filter.copy(bodyweightOnly = !filter.bodyweightOnly)) },
                label = { Text("Sin material") }
            )
        }
    }
}

/** Miniatura del ejercicio: la primera imagen, normalmente la posición inicial. */
@Composable
fun ExerciseThumbnail(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    val images = rememberExerciseImages(exercise)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(size)
    ) {
        when (val first = images.firstOrNull()) {
            is ExerciseImage.Local -> Image(
                painter = painterResource(first.resId),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            is ExerciseImage.Remote -> AsyncImage(
                model = first.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_exercise_placeholder)
            )

            null -> Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_exercise_placeholder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}

/**
 * Fila de ejercicio reutilizada por el catálogo y por el selector de la rutina: miniatura,
 * nombre, zona y material, el botón (i) y lo que cada pantalla quiera poner al final.
 */
@Composable
fun ExerciseRow(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    showInfoButton: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseThumbnail(exercise)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${exercise.equipment} · ${exercise.difficulty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showInfoButton) ExerciseInfoButton(exercise = exercise)
            trailing?.invoke()
        }
    }
}
