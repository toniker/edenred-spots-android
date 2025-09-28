package com.keremidis.edenredspots.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val name: String,
    val brandName: String
)

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places")
    suspend fun getAllPlaces(): List<PlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(places: List<PlaceEntity>)

    @Query("SELECT COUNT(*) FROM places")
    suspend fun getPlaceCount(): Int

    @Query("DELETE FROM places")
    suspend fun clearPlaces()
}