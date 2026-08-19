package com.raul.minimapagta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raul.minimapagta.data.model.PuntoDestacadoEntity
import com.raul.minimapagta.data.model.PuntoEntity

// Registramos las dos entidades que hemos programado hasta ahora
@Database(
    entities = [
        PuntoEntity::class,
        PuntoDestacadoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun puntoDao(): PuntoDao
    // abstract fun misionDao(): MisionDao (Las activaremos luego)
    // abstract fun rutinaDao(): RutinaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gta_map_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}