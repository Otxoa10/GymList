package com.gymlist.app.data.repo

import android.content.Context
import com.gymlist.app.data.model.Exercise
import org.json.JSONArray
import org.json.JSONObject

/**
 * Carga el catálogo de ejercicios que viene empaquetado con la app.
 *
 * Los datos son del catálogo libre de RepDB (https://repdb.co): 643 ejercicios con nombre,
 * descripción, instrucciones y consejos en español, y una o dos imágenes cada uno. La licencia
 * permite usarlos dentro de la app a cambio de una atribución visible, que está en la pantalla
 * "Acerca de". Ver LICENSE-RepDB-datos.md en la raíz del proyecto.
 */
object ExerciseCatalog {

    const val ASSET_JSON = "exercises_es.json"
    const val ASSET_IMAGE_DIR = "exercise_images"

    /**
     * Lee y traduce el catálogo. Se llama una sola vez, la primera vez que se abre la app, desde
     * un hilo de entrada/salida: son 700 KB de JSON.
     */
    fun load(context: Context): List<Exercise> {
        val raw = context.assets.open(ASSET_JSON).bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toExercise())
            }
        }
    }

    private fun JSONObject.toExercise(): Exercise {
        val bodyPartKey = text("bodyPart")
        return Exercise(
            id = getString("id"),
            name = text("name"),
            description = text("description"),
            muscleGroup = Taxonomy.bodyPart(bodyPartKey),
            bodyPartKey = bodyPartKey,
            // Los 195 ejercicios de peso corporal traen el material a null.
            equipment = Taxonomy.equipment(text("equipment")),
            difficulty = Taxonomy.difficulty(text("difficulty")),
            category = Taxonomy.category(text("category")),
            howTo = optJSONArray("instructions").toLines(),
            tips = optJSONArray("tips").toLines(),
            primaryMuscles = optJSONArray("primary").toStringList().map(Taxonomy::muscle),
            secondaryMuscles = optJSONArray("secondary").toStringList().map(Taxonomy::muscle),
            isBodyweight = optBoolean("bodyweight"),
            images = optJSONArray("images").toStringList()
        )
    }

    /**
     * `optString` de org.json devuelve literalmente la cadena "null" cuando el valor del JSON es
     * null, y eso acababa pintando "Null" como material. Esto devuelve cadena vacía.
     */
    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key)

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val value = optString(index)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    /** Los pasos se guardan como una sola cadena, un paso por línea. */
    private fun JSONArray?.toLines(): String = toStringList().joinToString("\n")
}

/**
 * Rutinas de ejemplo que se crean la primera vez, para que haya algo con lo que entrenar sin
 * tener que montar nada. Son editables y borrables como cualquier otra.
 */
object SeedRoutines {

    data class Item(
        val exerciseId: String,
        val sets: Int,
        val reps: String,
        val restSeconds: Int
    )

    data class Routine(
        val name: String,
        val notes: String,
        val items: List<Item>
    )

    val all: List<Routine> = listOf(
        Routine(
            name = "Full Body - iniciación",
            notes = "Para empezar: 2 o 3 días por semana, dejando un día de descanso entre medias.",
            items = listOf(
                Item("incline-treadmill-walk", 1, "8 min", 60),
                Item("goblet-squat", 3, "8-12", 90),
                Item("db-bench-press", 3, "8-12", 90),
                Item("seated-cable-row", 3, "10-12", 90),
                Item("dumbbell-shoulder-press", 3, "10-12", 75),
                Item("plank", 3, "30 s", 45)
            )
        ),
        Routine(
            name = "Día A - Torso",
            notes = "Empuje y tirón. Si acabas las series fáciles, sube algo de peso la próxima vez.",
            items = listOf(
                Item("db-bench-press", 4, "8-12", 90),
                Item("lat-pulldown", 4, "10-12", 90),
                Item("incline-db-press", 3, "10-12", 75),
                Item("single-arm-chest-supported-dumbbell-row", 3, "10-12", 75),
                Item("lateral-raise", 3, "12-15", 60),
                Item("bicep-curl", 3, "10-12", 60),
                Item("tricep-pushdown", 3, "10-12", 60)
            )
        ),
        Routine(
            name = "Día B - Pierna y core",
            notes = "Técnica primero y peso después, sobre todo en el peso muerto rumano.",
            items = listOf(
                Item("goblet-squat", 4, "8-12", 120),
                Item("leg-press", 3, "10-12", 90),
                Item("dumbbell-romanian-deadlift", 3, "10-12", 90),
                Item("db-lunge", 3, "10 por pierna", 75),
                Item("bodyweight-calf-raise", 4, "12-15", 45),
                Item("lying-leg-raise", 3, "12-15", 45),
                Item("plank", 3, "40 s", 45)
            )
        )
    )
}
