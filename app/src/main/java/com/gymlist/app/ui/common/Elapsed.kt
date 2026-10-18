package com.gymlist.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gymlist.app.util.formatClock
import kotlinx.coroutines.delay

/** Cronómetro ascendente del entrenamiento en curso, refrescado una vez por segundo. */
@Composable
fun rememberElapsedText(startedAt: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return formatClock(((now - startedAt) / 1000L).toInt())
}
