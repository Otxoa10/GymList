package com.gymlist.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.plan.Experience
import com.gymlist.app.data.plan.Focus
import com.gymlist.app.data.plan.Gear
import com.gymlist.app.data.plan.GeneratedPlan
import com.gymlist.app.data.plan.Goal
import com.gymlist.app.data.plan.Limitation
import com.gymlist.app.data.plan.PlanAnswers
import com.gymlist.app.data.plan.PlanGenerator
import com.gymlist.app.data.plan.Place
import com.gymlist.app.data.plan.SessionLength
import com.gymlist.app.data.repo.GymRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Las preguntas, en el orden en que se hacen. */
enum class PlanQuestion { GOAL, EXPERIENCE, DAYS, LENGTH, PLACE, LIMITATIONS, FOCUS }

/** Respuestas a medias: mientras se contesta, cualquiera puede faltar todavía. */
data class PlanDraft(
    val goal: Goal? = null,
    val experience: Experience? = null,
    val daysPerWeek: Int? = null,
    val length: SessionLength? = null,
    val place: Place? = null,
    val extraGear: Set<Gear> = emptySet(),
    val limitations: Set<Limitation> = emptySet(),
    val focus: Focus? = null
) {
    fun isAnswered(question: PlanQuestion): Boolean = when (question) {
        PlanQuestion.GOAL -> goal != null
        PlanQuestion.EXPERIENCE -> experience != null
        PlanQuestion.DAYS -> daysPerWeek != null
        PlanQuestion.LENGTH -> length != null
        PlanQuestion.PLACE -> place != null
        // No tener molestias también es una respuesta.
        PlanQuestion.LIMITATIONS -> true
        PlanQuestion.FOCUS -> focus != null
    }

    fun toAnswers(): PlanAnswers? = PlanAnswers(
        goal = goal ?: return null,
        experience = experience ?: return null,
        daysPerWeek = daysPerWeek ?: return null,
        length = length ?: return null,
        place = place ?: return null,
        extraGear = extraGear,
        limitations = limitations,
        focus = focus ?: return null
    )
}

enum class PlanPhase { QUESTIONS, REVIEW, SAVED }

data class PlanWizardUiState(
    val phase: PlanPhase = PlanPhase.QUESTIONS,
    val questionIndex: Int = 0,
    val draft: PlanDraft = PlanDraft(),
    val plan: GeneratedPlan? = null,
    val catalog: Map<String, Exercise> = emptyMap(),
    val saving: Boolean = false,
    val savedRoutineIds: List<Long> = emptyList(),
    /** Con un entrenamiento a medias no se ofrece empezar el plan: llevaría a ese otro. */
    val workoutInProgress: Boolean = false
) {
    val question: PlanQuestion get() = PlanQuestion.entries[questionIndex]
    val canContinue: Boolean get() = draft.isAnswered(question)
    val isLastQuestion: Boolean get() = questionIndex == PlanQuestion.entries.lastIndex
}

/**
 * El cuestionario y el plan que sale de él. El plan no se guarda hasta que el usuario lo
 * confirma: antes puede cambiar ejercicios o volver atrás a cambiar respuestas.
 */
class PlanWizardViewModel(private val repository: GymRepository) : ViewModel() {

    private val state = MutableStateFlow(PlanWizardUiState())

    val uiState: StateFlow<PlanWizardUiState> = combine(
        state,
        repository.observeExercises().map { list -> list.associateBy { it.id } },
        repository.observeActiveSession()
    ) { current, catalog, active ->
        current.copy(catalog = catalog, workoutInProgress = active != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanWizardUiState())

    fun answer(change: (PlanDraft) -> PlanDraft) {
        state.update { it.copy(draft = change(it.draft)) }
    }

    fun next() {
        val current = state.value
        if (current.phase != PlanPhase.QUESTIONS || !current.canContinue) return
        if (!current.isLastQuestion) {
            state.update { it.copy(questionIndex = it.questionIndex + 1) }
            return
        }
        val answers = current.draft.toAnswers() ?: return
        viewModelScope.launch {
            // Se lee el catálogo directamente: el de uiState puede no haber llegado todavía.
            val available = repository.observeExercises().first().mapTo(HashSet()) { it.id }
            val plan = PlanGenerator.generate(answers, available)
            state.update { it.copy(phase = PlanPhase.REVIEW, plan = plan) }
        }
    }

    /** Vuelve un paso atrás. Devuelve false si no hay paso anterior y toca cerrar la pantalla. */
    fun back(): Boolean {
        val current = state.value
        return when {
            current.phase == PlanPhase.REVIEW -> {
                state.update { it.copy(phase = PlanPhase.QUESTIONS, plan = null) }
                true
            }
            current.phase == PlanPhase.QUESTIONS && current.questionIndex > 0 -> {
                state.update { it.copy(questionIndex = it.questionIndex - 1) }
                true
            }
            else -> false
        }
    }

    fun swap(dayIndex: Int, exerciseIndex: Int) {
        state.update { current ->
            current.copy(plan = current.plan?.let { PlanGenerator.swap(it, dayIndex, exerciseIndex) })
        }
    }

    fun save() {
        val current = state.value
        val plan = current.plan ?: return
        if (current.saving || current.phase != PlanPhase.REVIEW) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val routineIds = repository.savePlan(plan)
            state.update {
                it.copy(saving = false, phase = PlanPhase.SAVED, savedRoutineIds = routineIds)
            }
        }
    }

    companion object {
        fun factory(repository: GymRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { PlanWizardViewModel(repository) }
        }
    }
}
