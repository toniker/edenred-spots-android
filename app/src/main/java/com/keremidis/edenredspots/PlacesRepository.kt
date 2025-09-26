package com.keremidis.edenredspots

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import javax.inject.Inject


class PlacesRepository @Inject constructor(@ApplicationContext val context: Context) {
    private val _places = MutableStateFlow(emptyList<Place>())
    val places: StateFlow<List<Place>> = _places
    private var loaded = false

    suspend fun loadPlaces(): StateFlow<List<Place>> {
        if (!loaded) {
            loaded = true
            _places.value = withContext(Dispatchers.IO) {
                context.resources.openRawResource(R.raw.response).use { inputStream ->
                    readPlaces(inputStream)
                }
            }
        }
        return places
    }

    private fun readPlaces(inputStream: InputStream): List<Place> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonPlaces = Json.decodeFromString<List<JsonPlace>>(jsonString)

        // Convert List<JsonPlace> to List<Place>
        return jsonPlaces.map { jsonPlace ->
            Place(
                id = jsonPlace.id,
                location = LatLng(jsonPlace.latitude, jsonPlace.longitude),
                address = formatAddress(jsonPlace), // Helper function for address
                name = jsonPlace.name,
                brandName = jsonPlace.brandName
            )
        }
    }

    // Helper function to construct the address string
    private fun formatAddress(jsonPlace: JsonPlace): String {
        val parts = mutableListOf<String>()
        parts.add(jsonPlace.address)
        jsonPlace.address2?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        jsonPlace.cityName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        jsonPlace.zipCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return parts.joinToString(", ").trimEnd(',', ' ')
    }
}
