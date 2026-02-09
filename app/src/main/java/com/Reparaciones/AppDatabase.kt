package com.Reparaciones

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Reparacion::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reparacionDao(): ReparacionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reparaciones_database" // Nombre del archivo .db
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}