package com.gymlist.app.ui.calendar

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.data.model.WorkoutSession
import com.gymlist.app.data.repo.GymRepository
import com.gymlist.app.ui.common.StatPill
import com.gymlist.app.util.epochDayToLocalDate
import com.gymlist.app.util.formatMinutes
import com.gymlist.app.util.formatVolume
import com.gymlist.app.util.formatWeight
import com.gymlist.app.util.millisToLocalTimeText
import com.gymlist.app.util.toReadable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val logs: List<SetLog> = emptyList()
) {
    val doneSets: Int get() = logs.count { it.done }
    val totalSets: Int get() = logs.size
    val volume: Double
        get() = logs.filter { it.done }.sumOf { (it.reps ?: 0) * (it.weight ?: 0.0) }

    /** Series agrupadas por ejercicio, en el orden en que se hicieron. */
    val byExercise: List<Pair<String, List<SetLog>>>
        get() = logs.groupBy { it.position }
            .toSortedMap()
            .map { (_, sets) -> sets.first().exerciseName to sets.sortedBy { it.setNumber } }
}

class SessionDetailViewModel(
    private val repository: GymRepository,
    private val sessionId: Long
) : ViewModel() {

    val uiState: StateFlow<SessionDetailUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeSetLogs(sessionId)
    ) { session, logs ->
        SessionDetailUiState(loading = false, session = session, logs = logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun updateNotes(notes: String) {
        viewModelScope.launch { repository.updateSessionNotes(sessionId, notes) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.discardSession(sessionId)
            onDeleted()
        }
    }

    companion object {
        fun factory(repository: GymRepository, sessionId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SessionDetailViewModel(repository, sessionId) }
            }
    }
}

/** Ficha de un entrenamiento ya hecho: qué series se completaron, con cuántas repes y cuánto peso. */
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        session?.routineName ?: "Entrenamiento",
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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Borrar este entrenamiento"
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
        if (session == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (!state.loading) Text("Este entrenamiento ya no existe")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    session.dayEpoch.epochDayToLocalDate().toReadable() +
                        " · ${session.startedAt.millisToLocalTimeText()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(
                        value = "${state.doneSets}/${state.totalSets}",
                        label = "series",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = formatMinutes(
                            session.finishedAt?.let { (it - session.startedAt) / 60_000L } ?: 0L
                        ),
                        label = "duración",
                        modifier = Modifier.weight(1f)
                    )
                    StatPill(
                        value = formatVolume(state.volume),
                        label = "volumen",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                NotesBox(initial = session.notes, onChange = viewModel::updateNotes)
            }

            state.byExercise.forEach { (name, sets) ->
                item(key = "ex-$name-${sets.first().id}") {
                    ExerciseLogCard(name = name, sets = sets)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Borrar este entrenamiento?") },
            text = { Text("Desaparecerá del calendario y de las estadísticas. No se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun NotesBox(initial: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    LaunchedEffect(initial) {
        if (text.isEmpty() && initial.isNotEmpty()) text = initial
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text("Notas (cómo te has sentido, molestias, etc.)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ExerciseLogCard(name: String, sets: List<SetLog>) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            sets.forEach { log ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (log.done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        contentColor = if (log.done) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (log.done) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (log.done) "completada" else "sin hacer",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Serie ${log.setNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        buildString {
                            append(log.reps?.toString() ?: "—")
                            append(" reps")
                            if (log.weight != null && log.weight > 0) {
                                append(" × ")
                                append(formatWeight(log.weight))
                                append(" kg")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (log.done) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "obj. ${log.targetReps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
