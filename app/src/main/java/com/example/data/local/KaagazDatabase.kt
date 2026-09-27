package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.FamilyMember
import com.example.data.model.Obligation
import com.example.data.model.ScannedDocument

@Database(
    entities = [
        FamilyMember::class,
        ScannedDocument::class,
        Obligation::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KaagazDatabase : RoomDatabase() {

    abstract fun kaagazDao(): KaagazDao

    companion object {
        @Volatile
        private var INSTANCE: KaagazDatabase? = null

        fun getDatabase(context: Context): KaagazDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaagazDatabase::class.java,
                    "kaagaz_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
