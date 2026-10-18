package com.gymlist.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.RoutineSummary
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.data.model.WorkoutSession
import com.gymlist.app.data.repo.GymRepository
import com.gymlist.app.util.TrainingStats
import com.gymlist.app.util.computeStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val activeSession: WorkoutSession? = null,
    val routines: List<RoutineSummary> = emptyList(),
    val recent: List<SessionSummary> = emptyList(),
    val stats: TrainingStats = TrainingStats()
)

class HomeViewModel(private val repository: GymRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeActiveSession(),
        repository.observeRoutineSummaries(),
        repository.observeFinishedSessions()
    ) { active, routines, finished ->
        HomeUiState(
            loading = false,
            activeSession = active,
            routines = routines,
            recent = finished.take(4),
            stats = computeStats(finished)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
