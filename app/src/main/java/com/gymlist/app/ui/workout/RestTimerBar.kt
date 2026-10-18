package com.gymlist.app.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymlist.app.ui.theme.TimerDigitsStyle
import com.gymlist.app.util.formatClock

/**
 * La barra del descanso. Se dibuja en el Scaffold principal, encima de la barra de navegación,
 * para que el tiempo siga a la vista aunque te vayas a mirar el calendario o cómo se hace el
 * siguiente ejercicio.
 */
@Composable
fun RestTimerBar(
    state: RestTimerState,
    onTogglePause: () -> Unit,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.active,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        val finished = state.finished
        val container =
            if (finished) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer
        val onContainer =
            if (finished) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onTertiaryContainer

        Surface(
            color = container,
            contentColor = onContainer,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownRing(
                    progress = state.progress,
                    text = formatClock(state.remainingSeconds),
                    finished = finished,
                    color = onContainer
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (finished) "¡Descanso terminado!" else "Descansando",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (finished) "A por la siguiente serie" else state.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AdjustButton("-15", onClick = { onAdjust(-15) }, color = onContainer)
                        Spacer(Modifier.width(6.dp))
                        AdjustButton("+15", onClick = { onAdjust(15) }, color = onContainer)
                        Spacer(Modifier.width(2.dp))
                        TextButton(onClick = onSkip) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = onContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (finished) "Cerrar" else "Saltar",
                                color = onContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                if (!finished) {
                    FilledIconButton(
                        onClick = onTogglePause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = onContainer.copy(alpha = 0.14f),
                            contentColor = onContainer
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            if (state.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.running) "Pausar descanso" else "Reanudar descanso",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(
    progress: Float,
    text: String,
    finished: Boolean,
    color: Color
) {
    // El anillo se anima entre segundos para que no vaya a saltos.
    val animated by animateFloatAsState(
        targetValue = if (finished) 1f else progress,
        animationSpec = tween(durationMillis = 250),
        label = "rest-progress"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(76.dp)
    ) {
        CircularProgressIndicator(
            progress = { animated },
            strokeWidth = 5.dp,
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            modifier = Modifier.size(76.dp)
        )
        Text(
            text,
            style = TimerDigitsStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
            color = color
        )
    }
}

@Composable
private fun AdjustButton(
    text: String,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        modifier = Modifier.size(width = 52.dp, height = 32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
