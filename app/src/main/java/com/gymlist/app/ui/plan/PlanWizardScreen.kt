package com.gymlist.app.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlist.app.data.plan.Experience
import com.gymlist.app.data.plan.Focus
import com.gymlist.app.data.plan.Gear
import com.gymlist.app.data.plan.Goal
import com.gymlist.app.data.plan.Limitation
import com.gymlist.app.data.plan.PlanGenerator
import com.gymlist.app.data.plan.Place
import com.gymlist.app.data.plan.SessionLength
import com.gymlist.app.ui.NoInsets

/**
 * Crear un plan personalizado: unas preguntas, una por pantalla, y al final el plan propuesto
 * para revisarlo antes de guardarlo como rutinas.
 */
@Composable
fun PlanWizardScreen(
    viewModel: PlanWizardViewModel,
    onClose: () -> Unit,
    onOpenRoutines: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // El botón atrás del sistema retrocede una pregunta en lugar de salir y perder las respuestas.
    BackHandler(
        enabled = state.phase == PlanPhase.REVIEW ||
            (state.phase == PlanPhase.QUESTIONS && state.questionIndex > 0)
    ) {
        viewModel.back()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.phase) {
                            PlanPhase.QUESTIONS -> "Crear mi plan"
                            PlanPhase.REVIEW -> "Tu plan"
                            PlanPhase.SAVED -> "Plan guardado"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                windowInsets = NoInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            when (state.phase) {
                PlanPhase.QUESTIONS -> QuestionActions(
                    showBack = state.questionIndex > 0,
                    isLast = state.isLastQuestion,
                    canContinue = state.canContinue,
                    onBack = { viewModel.back() },
                    onNext = viewModel::next
                )

                PlanPhase.REVIEW -> ReviewActions(
                    dayCount = state.plan?.days?.size ?: 0,
                    canSave = !state.saving && state.plan?.days?.any { it.exercises.isNotEmpty() } == true,
                    onSave = viewModel::save,
                    onChangeAnswers = { viewModel.back() }
                )

                PlanPhase.SAVED -> Unit
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.phase) {
                PlanPhase.QUESTIONS -> Questions(
                    questionIndex = state.questionIndex,
                    draft = state.draft,
                    onAnswer = viewModel::answer
                )

                PlanPhase.REVIEW -> state.plan?.let { plan ->
                    PlanReview(plan = plan, catalog = state.catalog, onSwap = viewModel::swap)
                }

                PlanPhase.SAVED -> PlanSaved(
                    routineCount = state.savedRoutineIds.size,
                    canStart = !state.workoutInProgress && state.savedRoutineIds.isNotEmpty(),
                    onStart = { state.savedRoutineIds.firstOrNull()?.let(onStartRoutine) },
                    onOpenRoutines = onOpenRoutines
                )
            }
        }
    }
}

@Composable
private fun Questions(
    questionIndex: Int,
    draft: PlanDraft,
    onAnswer: ((PlanDraft) -> PlanDraft) -> Unit
) {
    val total = PlanQuestion.entries.size

    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (questionIndex + 1f) / total },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        AnimatedContent(
            targetState = questionIndex,
            transitionSpec = {
                // Hacia la derecha al avanzar y hacia la izquierda al volver.
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { width -> direction * width / 4 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -direction * width / 4 } + fadeOut())
            },
            label = "pregunta"
        ) { index ->
            val question = PlanQuestion.entries[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Text(
                    "Pregunta ${index + 1} de $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(question.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    question.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                QuestionOptions(question = question, draft = draft, onAnswer = onAnswer)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QuestionOptions(
    question: PlanQuestion,
    draft: PlanDraft,
    onAnswer: ((PlanDraft) -> PlanDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (question) {
            PlanQuestion.GOAL -> Goal.entries.forEach { goal ->
                OptionCard(
                    title = goal.title,
                    detail = goal.detail,
                    icon = goal.icon,
                    selected = draft.goal == goal,
                    onClick = { onAnswer { it.copy(goal = goal) } }
                )
            }

            PlanQuestion.EXPERIENCE -> Experience.entries.forEach { experience ->
                OptionCard(
                    title = experience.title,
                    detail = experience.detail,
                    selected = draft.experience == experience,
                    onClick = { onAnswer { it.copy(experience = experience) } }
                )
            }

            PlanQuestion.DAYS -> {
                DaysPicker(
                    selected = draft.daysPerWeek,
                    onSelect = { days -> onAnswer { it.copy(daysPerWeek = days) } }
                )
                draft.daysPerWeek?.let { days ->
                    Spacer(Modifier.height(6.dp))
                    InfoLine(icon = Icons.Default.CalendarMonth, text = PlanGenerator.splitPreview(days))
                }
            }

            PlanQuestion.LENGTH -> SessionLength.entries.forEach { length ->
                OptionCard(
                    title = length.title,
                    detail = length.detail,
                    selected = draft.length == length,
                    onClick = { onAnswer { it.copy(length = length) } }
                )
            }

            PlanQuestion.PLACE -> {
                Place.entries.forEach { place ->
                    OptionCard(
                        title = place.title,
                        detail = place.detail,
                        icon = place.icon,
                        selected = draft.place == place,
                        onClick = { onAnswer { it.copy(place = place) } }
                    )
                }
                if (draft.place != null && draft.place != Place.GYM) {
                    Spacer(Modifier.height(10.dp))
                    Text("¿Tienes algo de esto?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Marca todo lo que tengas. Cuanto más material, más variedad de ejercicios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Gear.homeExtras.forEach { gear ->
                            val selected = gear in draft.extraGear
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onAnswer {
                                        it.copy(extraGear = if (selected) it.extraGear - gear else it.extraGear + gear)
                                    }
                                },
                                label = { Text(gear.title) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }

            PlanQuestion.LIMITATIONS -> {
                OptionCard(
                    title = "Ninguna",
                    detail = "Puedo hacer cualquier ejercicio",
                    selected = draft.limitations.isEmpty(),
                    onClick = { onAnswer { it.copy(limitations = emptySet()) } }
                )
                Limitation.entries.forEach { limitation ->
                    val selected = limitation in draft.limitations
                    OptionCard(
                        title = limitation.title,
                        selected = selected,
                        multiSelect = true,
                        onClick = {
                            onAnswer {
                                it.copy(
                                    limitations = if (selected) it.limitations - limitation
                                    else it.limitations + limitation
                                )
                            }
                        }
                    )
                }
            }

            PlanQuestion.FOCUS -> Focus.entries.forEach { focus ->
                OptionCard(
                    title = focus.title,
                    detail = focus.detail,
                    selected = draft.focus == focus,
                    onClick = { onAnswer { it.copy(focus = focus) } }
                )
            }
        }
    }
}

/** Tarjeta de respuesta: grande y fácil de tocar con una mano. */
@Composable
private fun OptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    detail: String? = null,
    icon: ImageVector? = null,
    multiSelect: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.45f),
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) colors.onPrimaryContainer else colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (detail != null) {
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                when {
                    multiSelect && selected -> Icons.Default.CheckBox
                    multiSelect -> Icons.Default.CheckBoxOutlineBlank
                    selected -> Icons.Default.CheckCircle
                    else -> Icons.Default.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (selected) colors.primary else colors.outline
            )
        }
    }
}

@Composable
private fun DaysPicker(selected: Int?, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (2..6).forEach { days ->
            val isSelected = days == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(days) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) colors.primary else colors.surfaceVariant.copy(alpha = 0.45f),
                contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                border = if (isSelected) null else BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.9f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("$days", style = MaterialTheme.typography.headlineMedium)
                    Text("días", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun InfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuestionActions(
    showBack: Boolean,
    isLast: Boolean,
    canContinue: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                TextButton(onClick = onBack) { Text("Atrás") }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onNext,
                enabled = canContinue,
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (isLast) "Ver mi plan" else "Siguiente")
            }
        }
    }
}

private val PlanQuestion.title: String
    get() = when (this) {
        PlanQuestion.GOAL -> "¿Qué quieres conseguir?"
        PlanQuestion.EXPERIENCE -> "¿Cuánta experiencia tienes?"
        PlanQuestion.DAYS -> "¿Cuántos días a la semana puedes entrenar?"
        PlanQuestion.LENGTH -> "¿Cuánto tiempo tienes por sesión?"
        PlanQuestion.PLACE -> "¿Dónde vas a entrenar?"
        PlanQuestion.LIMITATIONS -> "¿Tienes molestias en alguna zona?"
        PlanQuestion.FOCUS -> "¿Quieres dar prioridad a alguna zona?"
    }

private val PlanQuestion.subtitle: String
    get() = when (this) {
        PlanQuestion.GOAL -> "Elige lo que más te importe ahora. Decide las repeticiones, los descansos y si hay cardio."
        PlanQuestion.EXPERIENCE -> "Sirve para elegir ejercicios que puedas hacer bien desde el primer día."
        PlanQuestion.DAYS -> "Mejor los que puedas mantener todas las semanas que los que te gustaría."
        PlanQuestion.LENGTH -> "Contando el calentamiento. Con menos tiempo, el plan se queda con los ejercicios más importantes."
        PlanQuestion.PLACE -> "Solo se eligen ejercicios que puedas hacer con lo que tienes."
        PlanQuestion.LIMITATIONS -> "Se evitarán los ejercicios que suelen cargar esas zonas. Si tienes una lesión, consulta antes con un profesional."
        PlanQuestion.FOCUS -> "Se añade algo más de trabajo para esa zona, sin descuidar el resto."
    }

private val Goal.icon: ImageVector
    get() = when (this) {
        Goal.MUSCLE -> Icons.Default.FitnessCenter
        Goal.STRENGTH -> Icons.Default.Bolt
        Goal.FAT_LOSS -> Icons.Default.LocalFireDepartment
        Goal.HEALTH -> Icons.Default.Favorite
    }

private val Place.icon: ImageVector
    get() = when (this) {
        Place.GYM -> Icons.Default.FitnessCenter
        Place.HOME_DUMBBELLS -> Icons.Default.Home
        Place.HOME_BODYWEIGHT -> Icons.Default.SelfImprovement
    }
