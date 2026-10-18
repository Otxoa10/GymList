package com.gymlist.app.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.repo.ExerciseCatalog

/**
 * Una imagen de ejemplo de un ejercicio: o un drawable del propio APK, o algo que carga Coil
 * (un asset empaquetado, una URL o un fichero del móvil).
 */
sealed interface ExerciseImage {
    data class Local(@DrawableRes val resId: Int) : ExerciseImage
    data class Remote(val uri: String) : ExerciseImage
}

/**
 * Busca hasta 4 imágenes para un ejercicio, por este orden:
 *
 *  1. Drawables que haya puesto el usuario en `res/drawable/`, llamados `ex_<id>_1` … `ex_<id>_4`
 *     (con los guiones del id cambiados por guiones bajos, que es lo único que admiten los
 *     nombres de recurso: `goblet-squat` -> `ex_goblet_squat_1`). Tienen prioridad, así que
 *     sirven para sustituir la imagen del catálogo por una propia.
 *  2. Las imágenes del catálogo, empaquetadas en `assets/exercise_images/`.
 *
 * Todos los ejercicios del catálogo traen imagen, así que el caso de lista vacía solo se da en
 * ejercicios añadidos a mano.
 */
@Composable
fun rememberExerciseImages(exercise: Exercise): List<ExerciseImage> {
    val context = LocalContext.current
    return remember(exercise.id, exercise.images) {
        val resources = context.resources
        val packageName = context.packageName
        val resourceBase = exercise.id.replace('-', '_')

        val userProvided = (1..MAX_IMAGES).mapNotNull { index ->
            // getIdentifier permite la convención de nombres; res/raw/keep.xml evita que R8
            // elimine estos drawables al no verlos referenciados en el código.
            val id = resources.getIdentifier("ex_${resourceBase}_$index", "drawable", packageName)
            if (id != 0) ExerciseImage.Local(id) else null
        }
        if (userProvided.isNotEmpty()) return@remember userProvided

        exercise.images.take(MAX_IMAGES).map { entry ->
            // Una entrada puede ser un fichero del catálogo o, si alguien la edita, una URL.
            if (entry.startsWith("http") || entry.startsWith("file:") || entry.startsWith("content:")) {
                ExerciseImage.Remote(entry)
            } else {
                ExerciseImage.Remote("file:///android_asset/${ExerciseCatalog.ASSET_IMAGE_DIR}/$entry")
            }
        }
    }
}

const val MAX_IMAGES = 4
