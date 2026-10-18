package com.gymlist.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.Routine
import com.gymlist.app.data.model.RoutineDetail
import com.gymlist.app.data.model.RoutineItem
import com.gymlist.app.data.model.RoutineSummary
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: String): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(exercises: List<Exercise>)
}

@Dao
interface RoutineDao {

    @Query(
        """
        SELECT r.id AS id, r.name AS name, r.notes AS notes,
               COUNT(i.id) AS exerciseCount,
               COALESCE(SUM(i.sets), 0) AS totalSets
        FROM routines r
        LEFT JOIN routine_items i ON i.routineId = r.id
        GROUP BY r.id
        ORDER BY r.position, r.id
        """
    )
    fun observeSummaries(): Flow<List<RoutineSummary>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun observeDetail(routineId: Long): Flow<RoutineDetail?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun detailOnce(routineId: Long): RoutineDetail?

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM routines")
    suspend fun maxRoutinePosition(): Int

    @Insert
    suspend fun insertItem(item: RoutineItem): Long

    @Update
    suspend fun updateItem(item: RoutineItem)

    @Update
    suspend fun updateItems(items: List<RoutineItem>)

    @Delete
    suspend fun deleteItem(item: RoutineItem)

    @Query("SELECT COALESCE(MAX(position), -1) FROM routine_items WHERE routineId = :routineId")
    suspend fun maxItemPosition(routineId: Long): Int
}

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutSession?>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeOnce(): WorkoutSession?

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<WorkoutSession?>

    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("UPDATE sessions SET finishedAt = :finishedAt WHERE id = :sessionId")
    suspend fun finishSession(sessionId: Long, finishedAt: Long)

    @Query("UPDATE sessions SET notes = :notes WHERE id = :sessionId")
    suspend fun updateNotes(sessionId: Long, notes: String)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Insert
    suspend fun insertSetLogs(logs: List<SetLog>)

    @Insert
    suspend fun insertSetLog(log: SetLog): Long

    @Update
    suspend fun updateSetLog(log: SetLog)

    @Delete
    suspend fun deleteSetLog(log: SetLog)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY position, setNumber")
    fun observeSetLogs(sessionId: Long): Flow<List<SetLog>>

    /** Filtra también por `position`: una rutina puede repetir el mismo ejercicio dos veces. */
    @Query("SELECT COALESCE(MAX(setNumber), 0) FROM set_logs WHERE sessionId = :sessionId AND position = :position")
    suspend fun maxSetNumber(sessionId: Long, position: Int): Int

    /**
     * Último peso usado en ese ejercicio en un entrenamiento ya terminado. Sirve para
     * prerellenar las series nuevas y que no haya que recordarlo de una semana a otra.
     */
    @Query(
        """
        SELECT l.weight FROM set_logs l
        JOIN sessions s ON s.id = l.sessionId
        WHERE l.exerciseId = :exerciseId
          AND l.done = 1
          AND l.weight IS NOT NULL
          AND s.finishedAt IS NOT NULL
        ORDER BY s.startedAt DESC, l.setNumber ASC
        LIMIT 1
        """
    )
    suspend fun lastWeightFor(exerciseId: String): Double?

    @Query(
        """
        SELECT s.id AS id, s.routineName AS routineName, s.startedAt AS startedAt,
               s.finishedAt AS finishedAt, s.dayEpoch AS dayEpoch, s.notes AS notes,
               COALESCE(SUM(CASE WHEN l.done THEN 1 ELSE 0 END), 0) AS doneSets,
               COUNT(l.id) AS totalSets,
               COALESCE(SUM(CASE WHEN l.done THEN l.reps * COALESCE(l.weight, 0) ELSE 0 END), 0) AS volume
        FROM sessions s
        LEFT JOIN set_logs l ON l.sessionId = s.id
        WHERE s.finishedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        """
    )
    fun observeFinishedSummaries(): Flow<List<SessionSummary>>
}
