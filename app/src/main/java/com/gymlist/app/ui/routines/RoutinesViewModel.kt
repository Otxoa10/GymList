package com.gymlist.app.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.RoutineDetail
import com.gymlist.app.data.model.RoutineItem
import com.gymlist.app.data.model.RoutineSummary
import com.gymlist.app.data.repo.GymRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(private val repository: GymRepository) : ViewModel() {

    val routines: StateFlow<List<RoutineSummary>> = repository.observeRoutineSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createRoutine(name: String, onCreated: (Long) -> Unit) {
        val clean = name.trim().ifEmpty { "Rutina sin nombre" }
        viewModelScope.launch { onCreated(repository.createRoutine(clean)) }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch { repository.deleteRoutine(routineId) }
    }

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RoutinesViewModel(repository) }
        }
    }
}

data class RoutineDetailUiState(
    val loading: Boolean = true,
    val detail: RoutineDetail? = null,
    val catalog: List<Exercise> = emptyList(),
    /** Si hay un entrenamiento en marcha, el botón de empezar lleva a él en vez de crear otro. */
    val workoutInProgress: Boolean = false
)

class RoutineDetailViewModel(
    private val repository: GymRepository,
    private val routineId: Long
) : ViewModel() {

    val uiState: StateFlow<RoutineDetailUiState> = combine(
        repository.observeRoutineDetail(routineId),
        repository.observeExercises(),
        repository.observeActiveSession()
    ) { detail, catalog, active ->
        RoutineDetailUiState(
            loading = false,
            detail = detail,
            catalog = catalog,
            workoutInProgress = active != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineDetailUiState())

    fun rename(name: String) {
        val routine = uiState.value.detail?.routine ?: return
        viewModelScope.launch {
            repository.updateRoutine(routine.copy(name = name.trim().ifEmpty { routine.name }))
        }
    }

    fun updateNotes(notes: String) {
        val routine = uiState.value.detail?.routine ?: return
        viewModelScope.launch { repository.updateRoutine(routine.copy(notes = notes)) }
    }

    fun addExercise(exerciseId: String) {
        viewModelScope.launch { repository.addExerciseToRoutine(routineId, exerciseId) }
    }

    fun updateItem(item: RoutineItem) {
        viewModelScope.launch { repository.updateRoutineItem(item) }
    }

    fun deleteItem(item: RoutineItem) {
        viewModelScope.launch { repository.deleteRoutineItem(item) }
    }

    fun move(itemId: Long, delta: Int) {
        viewModelScope.launch { repository.moveRoutineItem(routineId, itemId, delta) }
    }

    fun deleteRoutine(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoutine(routineId)
            onDeleted()
        }
    }

    fun startWorkout(onStarted: () -> Unit) {
        viewModelScope.launch {
            repository.startSession(routineId)
            onStarted()
        }
    }

    companion object {
        fun factory(repository: GymRepository, routineId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { RoutineDetailViewModel(repository, routineId) }
            }
    }
}
