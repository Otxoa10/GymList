package com.gymlist.app.data.repo

import com.gymlist.app.data.local.ExerciseDao
import com.gymlist.app.data.local.RoutineDao
import com.gymlist.app.data.local.SessionDao
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.Routine
import com.gymlist.app.data.model.RoutineDetail
import com.gymlist.app.data.model.RoutineItem
import com.gymlist.app.data.model.RoutineSummary
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.data.model.WorkoutSession
import com.gymlist.app.data.plan.GeneratedPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GymRepository(
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val sessionDao: SessionDao
) {

    // --- Ejercicios ---

    fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()

    suspend fun exercise(id: String): Exercise? = exerciseDao.byId(id)

    /**
     * Inserta el catálogo y las rutinas de ejemplo la primera vez que se abre la app.
     *
     * El catálogo se inserta siempre (upsert) para que una actualización de la app traiga
     * ejercicios nuevos o textos corregidos; las rutinas de ejemplo solo se crean si no había
     * ninguna, para no resucitar las que el usuario haya borrado.
     */
    suspend fun seedCatalog(exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val firstRun = exerciseDao.count() == 0
        exerciseDao.upsertAll(exercises)
        if (!firstRun) return

        val known = exercises.mapTo(HashSet()) { it.id }
        SeedRoutines.all.forEachIndexed { index, seed ->
            val items = seed.items.filter { it.exerciseId in known }
            if (items.isEmpty()) return@forEachIndexed
            val routineId = routineDao.insertRoutine(
                Routine(name = seed.name, notes = seed.notes, position = index)
            )
            items.forEachIndexed { position, item ->
                routineDao.insertItem(
                    RoutineItem(
                        routineId = routineId,
                        exerciseId = item.exerciseId,
                        position = position,
                        sets = item.sets,
                        reps = item.reps,
                        restSeconds = item.restSeconds
                    )
                )
            }
        }
    }

    // --- Rutinas ---

    fun observeRoutineSummaries(): Flow<List<RoutineSummary>> = routineDao.observeSummaries()

    fun observeRoutineDetail(routineId: Long): Flow<RoutineDetail?> =
        routineDao.observeDetail(routineId)

    suspend fun createRoutine(name: String, notes: String = ""): Long =
        routineDao.insertRoutine(
            Routine(
                name = name,
                notes = notes,
                position = routineDao.maxRoutinePosition() + 1
            )
        )

    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routineId: Long) = routineDao.deleteRoutineById(routineId)

    suspend fun addExerciseToRoutine(
        routineId: Long,
        exerciseId: String,
        sets: Int = 3,
        reps: String = "8-12",
        restSeconds: Int = 90
    ) {
        routineDao.insertItem(
            RoutineItem(
                routineId = routineId,
                exerciseId = exerciseId,
                position = routineDao.maxItemPosition(routineId) + 1,
                sets = sets,
                reps = reps,
                restSeconds = restSeconds
            )
        )
    }

    suspend fun updateRoutineItem(item: RoutineItem) = routineDao.updateItem(item)

    suspend fun deleteRoutineItem(item: RoutineItem) {
        routineDao.deleteItem(item)
        // Al borrar queda un hueco en las posiciones; las recoloco para que el orden sea 0..n-1.
        routineDao.detailOnce(item.routineId)?.let { detail ->
            val renumbered = detail.orderedItems.mapIndexed { index, d ->
                d.item.copy(position = index)
            }
            if (renumbered.isNotEmpty()) routineDao.updateItems(renumbered)
        }
    }

    /**
     * Guarda cada día de un plan generado como una rutina normal, editable como cualquier otra.
     * Van al principio de la lista y en orden, porque es lo que el usuario acaba de pedir.
     * Devuelve los id de las rutinas, del día 1 al último.
     */
    suspend fun savePlan(plan: GeneratedPlan): List<Long> {
        val firstPosition = routineDao.minRoutinePosition() - plan.days.size
        return plan.days.mapIndexed { index, day ->
            val routineId = routineDao.insertRoutine(
                Routine(name = day.name, notes = day.notes, position = firstPosition + index)
            )
            day.exercises.forEachIndexed { position, exercise ->
                routineDao.insertItem(
                    RoutineItem(
                        routineId = routineId,
                        exerciseId = exercise.exerciseId,
                        position = position,
                        sets = exercise.sets,
                        reps = exercise.reps,
                        restSeconds = exercise.restSeconds
                    )
                )
            }
            routineId
        }
    }

    /** Mueve un ejercicio una posición arriba (-1) o abajo (+1) dentro de la rutina. */
    suspend fun moveRoutineItem(routineId: Long, itemId: Long, delta: Int) {
        val ordered = routineDao.detailOnce(routineId)?.orderedItems?.map { it.item } ?: return
        val from = ordered.indexOfFirst { it.id == itemId }
        val to = from + delta
        if (from < 0 || to < 0 || to > ordered.lastIndex) return
        val reordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        routineDao.updateItems(reordered.mapIndexed { index, item -> item.copy(position = index) })
    }

    // --- Entrenamiento en curso ---

    fun observeActiveSession(): Flow<WorkoutSession?> = sessionDao.observeActive()

    fun observeSession(sessionId: Long): Flow<WorkoutSession?> = sessionDao.observeSession(sessionId)

    fun observeSetLogs(sessionId: Long): Flow<List<SetLog>> = sessionDao.observeSetLogs(sessionId)

    /**
     * Crea el entrenamiento y, de una vez, todas sus series en blanco. Así marcar una serie es un
     * UPDATE, y si el entrenamiento se deja a medias queda registrado lo que faltó por hacer.
     * El peso se prerellena con el último que se usó en ese ejercicio.
     */
    suspend fun startSession(routineId: Long, today: LocalDate = LocalDate.now()): Long? {
        // Solo puede haber un entrenamiento en curso: si ya hay uno, se devuelve ese en lugar de
        // crear otro que dejaría al primero huérfano y sin terminar.
        sessionDao.activeOnce()?.let { return it.id }
        val detail = routineDao.detailOnce(routineId) ?: return null
        val now = System.currentTimeMillis()
        val sessionId = sessionDao.insertSession(
            WorkoutSession(
                routineId = routineId,
                routineName = detail.routine.name,
                startedAt = now,
                dayEpoch = today.toEpochDay()
            )
        )
        val logs = detail.orderedItems.flatMap { d ->
            val lastWeight = sessionDao.lastWeightFor(d.item.exerciseId)
            (1..d.item.sets).map { setNumber ->
                SetLog(
                    sessionId = sessionId,
                    exerciseId = d.item.exerciseId,
                    exerciseName = d.exercise.name,
                    position = d.item.position,
                    setNumber = setNumber,
                    targetReps = d.item.reps,
                    weight = lastWeight,
                    restSeconds = d.item.restSeconds
                )
            }
        }
        sessionDao.insertSetLogs(logs)
        return sessionId
    }

    suspend fun updateSetLog(log: SetLog) = sessionDao.updateSetLog(log)

    /** Añade una serie extra a un ejercicio ya empezado. */
    suspend fun addExtraSet(sessionId: Long, reference: SetLog) {
        val next = sessionDao.maxSetNumber(sessionId, reference.position) + 1
        sessionDao.insertSetLog(
            reference.copy(
                id = 0,
                setNumber = next,
                reps = null,
                done = false
            )
        )
    }

    suspend fun deleteSetLog(log: SetLog) = sessionDao.deleteSetLog(log)

    suspend fun finishSession(sessionId: Long) =
        sessionDao.finishSession(sessionId, System.currentTimeMillis())

    suspend fun updateSessionNotes(sessionId: Long, notes: String) =
        sessionDao.updateNotes(sessionId, notes)

    /** Descartar: borra el entrenamiento y sus series (en cascada). */
    suspend fun discardSession(sessionId: Long) = sessionDao.deleteSession(sessionId)

    // --- Histórico ---

    fun observeFinishedSessions(): Flow<List<SessionSummary>> = sessionDao.observeFinishedSummaries()
}
