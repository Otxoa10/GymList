package com.gymlist.app.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Catálogo de ejercicios. La clave es el slug del catálogo ("goblet-squat"), que sirve además
 * para localizar sus imágenes.
 *
 * Los textos llegan ya en español desde `assets/exercises_es.json`; las categorías
 * (zona, material, dificultad, músculos) se traducen al cargarlas con [com.gymlist.app.data.repo.Taxonomy].
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    /** Una frase de qué es el ejercicio y para qué sirve. */
    val description: String,
    /** Zona del cuerpo, ya traducida. Es como se agrupa el listado. */
    val muscleGroup: String,
    /** Clave original de la zona, para filtrar y ordenar sin depender del texto. */
    val bodyPartKey: String,
    val equipment: String,
    val difficulty: String,
    val category: String,
    /** Pasos de ejecución, uno por línea. */
    val howTo: String,
    /** Errores habituales y consejos, uno por línea. */
    val tips: String,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val isBodyweight: Boolean = false,
    /**
     * Imágenes del ejercicio: nombres de fichero dentro de `assets/exercise_images/`, en orden
     * (posición inicial y final). Ver [com.gymlist.app.util.rememberExerciseImages].
     */
    val images: List<String> = emptyList()
)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val position: Int = 0
)

/** Un ejercicio dentro de una rutina, con sus series / repes / descanso configurados. */
@Entity(
    tableName = "routine_items",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: String,
    val position: Int,
    val sets: Int,
    /** Texto libre porque un principiante trabaja por rangos: "8-12", "10", "30 s". */
    val reps: String,
    val restSeconds: Int
)

/**
 * Un entrenamiento. `finishedAt == null` significa que está en curso: solo puede haber uno así,
 * y es lo que la pantalla de inicio recupera al volver a abrir la app.
 */
@Entity(tableName = "sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long?,
    /** Copia del nombre: si luego se renombra o borra la rutina, el histórico no cambia. */
    val routineName: String,
    val startedAt: Long,
    /** `LocalDate.toEpochDay()`, para agrupar por día en el calendario sin hacer cuentas en SQL. */
    val dayEpoch: Long,
    val finishedAt: Long? = null,
    val notes: String = ""
)

/**
 * Una serie concreta de un entrenamiento. Se crean todas al empezar (desmarcadas), así marcar
 * una serie es un simple UPDATE y el histórico queda completo aunque se deje a medias.
 */
@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    /** Desnormalizado por el mismo motivo que `routineName`. */
    val exerciseName: String,
    /** Orden del ejercicio dentro del entrenamiento. */
    val position: Int,
    val setNumber: Int,
    val targetReps: String,
    val reps: Int? = null,
    val weight: Double? = null,
    val done: Boolean = false,
    val restSeconds: Int = 90
)

// --- Proyecciones de lectura ---

data class RoutineSummary(
    val id: Long,
    val name: String,
    val notes: String,
    val exerciseCount: Int,
    val totalSets: Int
)

data class RoutineItemDetail(
    @Embedded val item: RoutineItem,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise
)

data class RoutineDetail(
    @Embedded val routine: Routine,
    @Relation(entity = RoutineItem::class, parentColumn = "id", entityColumn = "routineId")
    val items: List<RoutineItemDetail>
) {
    /** @Relation no garantiza el orden, así que lo aplico aquí. */
    val orderedItems: List<RoutineItemDetail> get() = items.sortedBy { it.item.position }
}

data class SessionSummary(
    val id: Long,
    val routineName: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val dayEpoch: Long,
    val notes: String,
    val doneSets: Int,
    val totalSets: Int,
    /** Suma de repes × kg de las series marcadas. 0 si se entrena sin peso. */
    val volume: Double
) {
    val durationMinutes: Long
        get() = finishedAt?.let { (it - startedAt) / 60_000L } ?: 0L
}
