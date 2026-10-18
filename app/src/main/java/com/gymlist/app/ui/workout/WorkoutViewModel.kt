package com.gymlist.app.ui.workout

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.data.model.WorkoutSession
import com.gymlist.app.data.repo.GymRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** Estado del temporizador de descanso. Vive en el ViewModel para que siga contando al cambiar de pestaña. */
data class RestTimerState(
    val active: Boolean = false,
    val label: String = "",
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val running: Boolean = false
) {
    val finished: Boolean get() = active && remainingSeconds <= 0

    val progress: Float
        get() = if (totalSeconds <= 0) 0f
        else (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

/** Las series de un mismo ejercicio dentro del entrenamiento. */
data class ExerciseBlock(
    val position: Int,
    val exerciseId: String,
    val name: String,
    val sets: List<SetLog>
) {
    val doneSets: Int get() = sets.count { it.done }
    val complete: Boolean get() = sets.isNotEmpty() && doneSets == sets.size
    val targetReps: String get() = sets.firstOrNull()?.targetReps.orEmpty()
    val restSeconds: Int get() = sets.firstOrNull()?.restSeconds ?: 90
}

data class WorkoutUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val catalog: Map<String, Exercise> = emptyMap()
) {
    val totalSets: Int get() = blocks.sumOf { it.sets.size }
    val doneSets: Int get() = blocks.sumOf { it.doneSets }
    val progress: Float get() = if (totalSets == 0) 0f else doneSets.toFloat() / totalSets
    val allDone: Boolean get() = totalSets > 0 && doneSets == totalSets

    /** Primer ejercicio sin terminar: es el que la pantalla deja desplegado. */
    val currentPosition: Int? get() = blocks.firstOrNull { !it.complete }?.position
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val repository: GymRepository) : ViewModel() {

    val uiState: StateFlow<WorkoutUiState> =
        combine(repository.observeActiveSession(), repository.observeExercises()) { session, exercises ->
            session to exercises.associateBy { it.id }
        }.flatMapLatest { (session, catalog) ->
            if (session == null) {
                flowOf(WorkoutUiState(loading = false, catalog = catalog))
            } else {
                repository.observeSetLogs(session.id).map { logs ->
                    WorkoutUiState(
                        loading = false,
                        session = session,
                        blocks = logs.toBlocks(),
                        catalog = catalog
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val _timer = MutableStateFlow(RestTimerState())
    val timer: StateFlow<RestTimerState> = _timer.asStateFlow()

    /** Aviso para que la pantalla vibre y suene: el ViewModel no toca el Context. */
    private val _restFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restFinished = _restFinished.asSharedFlow()

    private var tickerJob: Job? = null

    /**
     * Instante de fin en el reloj monótono del sistema (no en la hora del móvil), para que el
     * descanso no se descuadre si cambia la zona horaria o la hora mientras cuenta.
     */
    private var deadline = 0L

    // --- Entrenamiento ---

    fun startRoutine(routineId: Long, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.startSession(routineId)
            onStarted()
        }
    }

    /**
     * Marca o desmarca una serie. Al marcarla, si no se han escrito las repeticiones se rellenan
     * con el primer número del objetivo ("8-12" -> 8), y arranca el descanso de ese ejercicio.
     */
    fun toggleSet(log: SetLog) {
        viewModelScope.launch {
            val nowDone = !log.done
            val reps = log.reps ?: if (nowDone) firstNumberIn(log.targetReps) else null
            repository.updateSetLog(log.copy(done = nowDone, reps = reps))
            if (nowDone && log.restSeconds > 0) {
                startRest(log.restSeconds, "${log.exerciseName} · serie ${log.setNumber}")
            }
        }
    }

    fun setReps(log: SetLog, text: String) {
        viewModelScope.launch {
            repository.updateSetLog(log.copy(reps = text.toIntOrNull()))
        }
    }

    fun setWeight(log: SetLog, text: String) {
        viewModelScope.launch {
            repository.updateSetLog(log.copy(weight = text.toDoubleOrNull()))
        }
    }

    fun addSet(block: ExerciseBlock) {
        val reference = block.sets.lastOrNull() ?: return
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch { repository.addExtraSet(sessionId, reference) }
    }

    fun removeLastSet(block: ExerciseBlock) {
        // Nunca dejo un ejercicio con cero series: para eso se edita la rutina.
        if (block.sets.size <= 1) return
        val last = block.sets.last()
        viewModelScope.launch { repository.deleteSetLog(last) }
    }

    fun finishWorkout(onFinished: () -> Unit = {}) {
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            repository.finishSession(sessionId)
            skipRest()
            onFinished()
        }
    }

    fun discardWorkout(onDiscarded: () -> Unit = {}) {
        val sessionId = uiState.value.session?.id ?: return
        viewModelScope.launch {
            repository.discardSession(sessionId)
            skipRest()
            onDiscarded()
        }
    }

    // --- Descanso ---

    fun startRest(seconds: Int, label: String) {
        if (seconds <= 0) return
        deadline = SystemClock.elapsedRealtime() + seconds * 1000L
        _timer.value = RestTimerState(
            active = true,
            label = label,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            running = true
        )
        startTicker()
    }

    fun togglePause() {
        val state = _timer.value
        if (!state.active || state.finished) return
        if (state.running) {
            tickerJob?.cancel()
            _timer.value = state.copy(running = false)
        } else {
            deadline = SystemClock.elapsedRealtime() + state.remainingSeconds * 1000L
            _timer.value = state.copy(running = true)
            startTicker()
        }
    }

    /** Los botones -15 s / +15 s. También reanuda si el descanso ya había terminado. */
    fun adjustRest(deltaSeconds: Int) {
        val state = _timer.value
        if (!state.active) return
        val remaining = (state.remainingSeconds + deltaSeconds).coerceIn(0, 59 * 60)
        deadline = SystemClock.elapsedRealtime() + remaining * 1000L
        _timer.value = state.copy(
            remainingSeconds = remaining,
            totalSeconds = maxOf(state.totalSeconds, remaining),
            running = remaining > 0
        )
        if (remaining > 0) startTicker() else tickerJob?.cancel()
    }

    fun skipRest() {
        tickerJob?.cancel()
        _timer.value = RestTimerState()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val remainingMillis = deadline - SystemClock.elapsedRealtime()
                val remaining = ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(0)
                _timer.value = _timer.value.copy(remainingSeconds = remaining)
                if (remaining <= 0) {
                    _timer.value = _timer.value.copy(running = false)
                    _restFinished.tryEmit(Unit)
                    break
                }
                // 200 ms: suficiente para que el segundo cambie a tiempo sin recomponer de más.
                delay(200)
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkoutViewModel(repository) }
        }
    }
}

private val numberRegex = Regex("\\d+")

private fun firstNumberIn(text: String): Int? = numberRegex.find(text)?.value?.toIntOrNull()

/**
 * Agrupa las series por ejercicio. La clave es la posición, no el id: una rutina puede incluir
 * el mismo ejercicio dos veces y cada aparición es un bloque distinto.
 */
private fun List<SetLog>.toBlocks(): List<ExerciseBlock> =
    groupBy { it.position }
        .toSortedMap()
        .map { (position, logs) ->
            ExerciseBlock(
                position = position,
                exerciseId = logs.first().exerciseId,
                name = logs.first().exerciseName,
                sets = logs.sortedBy { it.setNumber }
            )
        }
