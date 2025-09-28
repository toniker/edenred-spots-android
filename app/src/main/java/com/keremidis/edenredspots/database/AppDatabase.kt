package com.keremidis.edenredspots.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PlaceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}

