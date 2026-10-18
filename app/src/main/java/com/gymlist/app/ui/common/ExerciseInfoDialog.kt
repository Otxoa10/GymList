package com.gymlist.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gymlist.app.R
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.util.ExerciseImage
import com.gymlist.app.util.rememberExerciseImages

/**
 * Lo que se abre con el botón (i): imágenes de la posición inicial y final, para qué sirve el
 * ejercicio, qué músculos trabaja, los pasos de ejecución y los fallos típicos. Es la ayuda
 * para quien acaba de empezar y no sabe cómo se hace.
 */
@Composable
fun ExerciseInfoDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${exercise.muscleGroup} · ${exercise.equipment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                ExerciseImageCarousel(exercise)

                if (exercise.description.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        exercise.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(exercise.difficulty, highlighted = true)
                    Tag(exercise.category)
                    if (exercise.isBodyweight) Tag("Sin material")
                }

                if (exercise.primaryMuscles.isNotEmpty() || exercise.secondaryMuscles.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("Músculos que trabaja", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        exercise.primaryMuscles.forEach { Tag(it, highlighted = true) }
                        exercise.secondaryMuscles.forEach { Tag(it) }
                    }
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "En verde los principales; el resto ayudan.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val steps = exercise.howTo.lines().filter { it.isNotBlank() }
                if (steps.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    InstructionBlock(title = "Cómo se hace", lines = steps, numbered = true)
                }

                val tips = exercise.tips.lines().filter { it.isNotBlank() }
                if (tips.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(20.dp))
                    InstructionBlock(title = "A tener en cuenta", lines = tips, numbered = false)
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Entendido") }
            }
        }
    }
}

@Composable
private fun ExerciseImageCarousel(exercise: Exercise) {
    val images = rememberExerciseImages(exercise)

    if (images.isEmpty()) {
        NoImagesPlaceholder(exercise.id)
        return
    }

    val pagerState = rememberPagerState(pageCount = { images.size })
    // El catálogo da la posición inicial y la final, en ese orden.
    val labels = if (images.size == 2) listOf("Posición inicial", "Posición final") else emptyList()

    Column {
        Box {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) { page ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (val image = images[page]) {
                        is ExerciseImage.Local -> Image(
                            painter = painterResource(image.resId),
                            contentDescription = "Ejemplo ${page + 1} de ${exercise.name}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )

                        is ExerciseImage.Remote -> AsyncImage(
                            model = image.uri,
                            contentDescription = labels.getOrNull(page)
                                ?: "Ejemplo ${page + 1} de ${exercise.name}",
                            contentScale = ContentScale.Fit,
                            placeholder = painterResource(R.drawable.ic_exercise_placeholder),
                            error = painterResource(R.drawable.ic_exercise_placeholder),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        labels.getOrNull(pagerState.currentPage)
                            ?: "${pagerState.currentPage + 1}/${images.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (images.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(images.size) { index ->
                    val selected = index == pagerState.currentPage
                    val color by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        label = "dot"
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                            .background(color, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Solo aparece en ejercicios añadidos a mano: los 643 del catálogo traen imagen. Explica cómo
 * ponerle una en lugar de dejar un hueco vacío.
 */
@Composable
private fun NoImagesPlaceholder(exerciseId: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ImageNotSupported,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Este ejercicio no tiene imágenes",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Copia de 1 a 4 imágenes en res/drawable con estos nombres y aparecerán aquí:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "ex_${exerciseId.replace('-', '_')}_1 … _4",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun InstructionBlock(
    title: String,
    lines: List<String>,
    numbered: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!numbered) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        lines.forEachIndexed { index, line ->
            Row(Modifier.padding(bottom = 10.dp)) {
                if (numbered) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                } else {
                    Text(
                        "•",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }
                Text(
                    line.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Etiqueta compacta: dificultad, categoría, músculo. En verde las destacadas. */
@Composable
fun Tag(
    text: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 3.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
