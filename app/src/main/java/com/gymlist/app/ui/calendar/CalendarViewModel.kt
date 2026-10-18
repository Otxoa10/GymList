package com.gymlist.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.data.repo.GymRepository
import com.gymlist.app.util.TrainingStats
import com.gymlist.app.util.computeStats
import com.gymlist.app.util.epochDayToLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CalendarUiState(
    val loading: Boolean = true,
    /** Entrenamientos terminados, agrupados por el día en que se hicieron. */
    val byDay: Map<LocalDate, List<SessionSummary>> = emptyMap(),
    val stats: TrainingStats = TrainingStats()
) {
    fun sessionsOn(date: LocalDate): List<SessionSummary> = byDay[date].orEmpty()
}

class CalendarViewModel(private val repository: GymRepository) : ViewModel() {

    val uiState: StateFlow<CalendarUiState> = repository.observeFinishedSessions()
        .map { sessions ->
            CalendarUiState(
                loading = false,
                byDay = sessions.groupBy { it.dayEpoch.epochDayToLocalDate() },
                stats = computeStats(sessions)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CalendarViewModel(repository) }
        }
    }
}
