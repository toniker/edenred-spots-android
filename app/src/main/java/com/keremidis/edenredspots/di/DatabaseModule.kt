package com.keremidis.edenredspots.di

import android.content.Context
import androidx.room.Room
import com.keremidis.edenredspots.database.AppDatabase
import com.keremidis.edenredspots.database.PlaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "places_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePlaceDao(appDatabase: AppDatabase): PlaceDao {
        return appDatabase.placeDao()
    }
}
