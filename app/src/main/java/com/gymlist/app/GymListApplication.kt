package com.gymlist.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.gymlist.app.data.local.GymListDatabase
import com.gymlist.app.data.repo.ExerciseCatalog
import com.gymlist.app.data.repo.GymRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias a mano: la app es pequeña y un contenedor de dos objetos se entiende
 * mejor que montar Hilt para esto.
 */
class AppContainer(context: Context) {
    private val database = GymListDatabase.get(context)

    val repository = GymRepository(
        exerciseDao = database.exerciseDao(),
        routineDao = database.routineDao(),
        sessionDao = database.sessionDao()
    )
}

class GymListApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Catálogo de ejercicios (y, la primera vez, rutinas de ejemplo). Va en un hilo de
        // entrada/salida porque son 700 KB de JSON y 643 inserciones.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { container.repository.seedCatalog(ExerciseCatalog.load(this@GymListApplication)) }
                .onFailure { Log.e("GymList", "No se pudo cargar el catálogo de ejercicios", it) }
        }
    }
}
