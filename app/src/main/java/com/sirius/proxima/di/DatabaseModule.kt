package com.sirius.proxima.di

import android.content.Context
import androidx.room.Room
import com.sirius.proxima.data.database.ProximaDatabase
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.datastore.dataStore
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.data.repository.TimetableRepository

object ServiceLocator {

    @Volatile
    private var database: ProximaDatabase? = null

    @Volatile
    private var settingsDataStore: SettingsDataStore? = null

    @Volatile
    private var subjectRepository: SubjectRepository? = null

    @Volatile
    private var timetableRepository: TimetableRepository? = null

    private fun getDatabase(context: Context): ProximaDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                ProximaDatabase::class.java,
                "proxima_database"
            ).fallbackToDestructiveMigration(true).build().also { database = it }
        }
    }

    fun getSettingsDataStore(context: Context): SettingsDataStore {
        return settingsDataStore ?: synchronized(this) {
            settingsDataStore ?: SettingsDataStore(context.applicationContext.dataStore).also {
                settingsDataStore = it
            }
        }
    }

    fun getSubjectRepository(context: Context): SubjectRepository {
        return subjectRepository ?: synchronized(this) {
            val db = getDatabase(context)
            subjectRepository ?: SubjectRepository(
                db.subjectDao(), db.timetableEntryDao()
            ).also { subjectRepository = it }
        }
    }

    fun getTimetableRepository(context: Context): TimetableRepository {
        return timetableRepository ?: synchronized(this) {
            val db = getDatabase(context)
            timetableRepository ?: TimetableRepository(
                db.timetableEntryDao()
            ).also { timetableRepository = it }
        }
    }
}
