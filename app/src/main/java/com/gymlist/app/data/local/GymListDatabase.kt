package com.gymlist.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.Routine
import com.gymlist.app.data.model.RoutineItem
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.data.model.WorkoutSession

@Database(
    entities = [Exercise::class, Routine::class, RoutineItem::class, WorkoutSession::class, SetLog::class],
    // 2: el catálogo pasó de 20 ejercicios escritos a mano a los 643 de assets/exercises_es.json.
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GymListDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var instance: GymListDatabase? = null

        fun get(context: Context): GymListDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymListDatabase::class.java,
                    "gymlist.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
