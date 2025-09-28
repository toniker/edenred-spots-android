package com.keremidis.edenredspots

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.keremidis.edenredspots.database.PlaceDao
import com.keremidis.edenredspots.database.PlaceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placeDao: PlaceDao
) {
    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    suspend fun loadPlaces() {
        withContext(Dispatchers.IO) {
            if (placeDao.getPlaceCount() == 0) {
                val parsedPlaces = context.resources.openRawResource(R.raw.response).use { inputStream ->
                    readPlaces(inputStream)
                }
                placeDao.insertPlaces(parsedPlaces.map { it.toEntity() })
            }
            // Update the StateFlow once data is loaded or available from DAO
            _places.value = placeDao.getAllPlaces().map { entity ->
                entity.toDomain()
            }
        }
    }

    private fun readPlaces(inputStream: InputStream): List<Place> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonPlaces = jsonDecoder.decodeFromString<List<JsonPlace>>(jsonString)
        return jsonPlaces.map { jsonPlace ->
            Place(
                id = jsonPlace.id,
                location = LatLng(jsonPlace.latitude, jsonPlace.longitude),
                address = formatAddress(jsonPlace),
                name = jsonPlace.name,
                brandName = jsonPlace.brandName
            )
        }
    }

    private fun PlaceEntity.toDomain() = Place(
        id = id,
        location = LatLng(latitude, longitude),
        address = address,
        name = name,
        brandName = brandName
    )

    private fun Place.toEntity() = PlaceEntity(
        id = id,
        latitude = location.latitude,
        longitude = location.longitude,
        address = address,
        name = name,
        brandName = brandName
    )

    private fun formatAddress(jsonPlace: JsonPlace): String {
        val parts = mutableListOf<String>()
        parts.add(jsonPlace.address)
        jsonPlace.address2?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        jsonPlace.cityName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        jsonPlace.zipCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString(", ").trimEnd(',', ' ')
    }
}
