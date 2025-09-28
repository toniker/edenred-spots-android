package com.keremidis.edenredspots

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.keremidis.edenredspots.ui.theme.EdenredSpotsTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@AndroidEntryPoint
class MapActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EdenredSpotsTheme {
                MapScreen(viewModel)
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun MapScreen(viewModel: MapViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(Place.DEFAULT_LAT_LNG, 10f)
    }
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val isDarkTheme = isSystemInDarkTheme()

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Load places and get user location
    LaunchedEffect(Unit) {
        viewModel.loadPlaces()
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLocation!!,  16f))
                    }
                }
        }
    }

    // Handle zoom commands from ViewModel
    LaunchedEffect(uiState.zoomCommand) {
        uiState.zoomCommand?.let { command ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(command.target, command.zoom)
            )
            viewModel.clearZoomCommand()
        }
    }

    // Update visible places when camera moves
    LaunchedEffect(cameraPositionState.position) {
        val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
        bounds?.let {
            viewModel.updateVisiblePlaces(it, cameraPositionState.position.zoom)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapStyleOptions = if (isDarkTheme) {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } else null
            ),
            uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
            onMapLoaded = {
                // Initial load of visible places
                val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                bounds?.let {
                    viewModel.updateVisiblePlaces(it, cameraPositionState.position.zoom)
                }
            },
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
            // Display clusters or individual markers based on zoom level
            if (cameraPositionState.position.zoom >= 16f) {
                // Show individual markers only when very zoomed in
                uiState.visiblePlaces.forEach { place ->
                    Marker(
                        state = MarkerState(position = place.location),
                        title = place.name,
                        snippet = place.brandName,
                        onClick = { marker ->
                            viewModel.selectPlace(place)
                            false
                        }
                    )
                }
            } else {
                // Show clusters when zoomed out
                uiState.clusters.forEach { cluster ->
                    Marker(
                        state = MarkerState(position = cluster.center),
                        title = "${cluster.count} places",
                        snippet = "Tap to zoom in",
                        onClick = { marker ->
                            // Zoom in on cluster
                            viewModel.zoomToCluster(
                                cluster.center,
                                cameraPositionState.position.zoom + 2f
                            )
                            false
                        }
                    )
                }
            }
        }

        // Bottom sheet for selected place
        uiState.selectedPlace?.let { selectedPlace ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = selectedPlace.brandName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedPlace.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedPlace.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { viewModel.clearSelection() }
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var allPlaces: List<Place> = emptyList()

    fun loadPlaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                placesRepository.places.collect { places ->
                    allPlaces = places
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allPlaces = places
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateVisiblePlaces(bounds: LatLngBounds, zoomLevel: Float) {
        val placesInBounds = allPlaces.filter { place ->
            bounds.contains(place.location)
        }

        if (zoomLevel >= 16f) {
            // Show individual places when zoomed in very close
            val visiblePlaces = cullByDistance(placesInBounds).take(50)
            _uiState.value = _uiState.value.copy(
                visiblePlaces = visiblePlaces,
                clusters = emptyList()
            )
        } else {
            // Create clusters when zoomed out
            val clusters = createClusters(placesInBounds, zoomLevel)
            _uiState.value = _uiState.value.copy(
                visiblePlaces = emptyList(),
                clusters = clusters
            )
        }
    }

    fun selectPlace(place: Place) {
        _uiState.value = _uiState.value.copy(selectedPlace = place)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedPlace = null)
    }

    fun zoomToCluster(clusterCenter: LatLng, zoomLevel: Float) {
        _uiState.value = _uiState.value.copy(
            zoomCommand = ZoomCommand(target = clusterCenter, zoom = zoomLevel)
        )
    }

    fun clearZoomCommand() {
        _uiState.value = _uiState.value.copy(zoomCommand = null)
    }

    private fun createClusters(places: List<Place>, zoomLevel: Float): List<PlaceCluster> {
        if (places.isEmpty()) return emptyList()

        // Determine clustering radius based on zoom level (more aggressive clustering)
        val clusterRadius = when {
            zoomLevel >= 14f -> 200.0  // 200m radius
            zoomLevel >= 12f -> 500.0  // 500m radius
            zoomLevel >= 10f -> 1000.0 // 1km radius
            zoomLevel >= 8f -> 2000.0  // 2km radius
            else -> 5000.0             // 5km radius
        }

        val clusters = mutableListOf<PlaceCluster>()
        val unclusteredPlaces = places.toMutableList()

        while (unclusteredPlaces.isNotEmpty()) {
            val centerPlace = unclusteredPlaces.removeAt(0)
            val nearbyPlaces = mutableListOf(centerPlace)

            // Find all places within cluster radius
            val iterator = unclusteredPlaces.iterator()
            while (iterator.hasNext()) {
                val place = iterator.next()
                if (distanceBetween(centerPlace.location, place.location) <= clusterRadius) {
                    nearbyPlaces.add(place)
                    iterator.remove()
                }
            }

            // Create cluster
            clusters.add(
                PlaceCluster(
                    center = calculateCentroid(nearbyPlaces),
                    count = nearbyPlaces.size,
                    places = nearbyPlaces
                )
            )
        }

        // Limit number of clusters for performance
        return clusters.take(50)
    }

    private fun calculateCentroid(places: List<Place>): LatLng {
        if (places.isEmpty()) return Place.DEFAULT_LAT_LNG

        val avgLat = places.map { it.location.latitude }.average()
        val avgLng = places.map { it.location.longitude }.average()

        return LatLng(avgLat, avgLng)
    }

    private fun cullByDistance(places: List<Place>): List<Place> {
        val minDistanceMeters = 50.0
        if (places.isEmpty()) return emptyList()

        val result = mutableListOf<Place>()
        result.add(places.first())

        for (place in places.drop(1)) {
            val isFarEnough = result.all { existingPlace ->
                distanceBetween(place.location, existingPlace.location) >= minDistanceMeters
            }
            if (isFarEnough) {
                result.add(place)
            }
        }

        return result
    }

    private fun distanceBetween(pos1: LatLng, pos2: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        val lat1Rad = Math.toRadians(pos1.latitude)
        val lat2Rad = Math.toRadians(pos2.latitude)
        val deltaLatRad = Math.toRadians(pos2.latitude - pos1.latitude)
        val deltaLngRad = Math.toRadians(pos2.longitude - pos1.longitude)

        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}

data class PlaceCluster(
    val center: LatLng,
    val count: Int,
    val places: List<Place>
)

data class MapUiState(
    val isLoading: Boolean = true,
    val allPlaces: List<Place> = emptyList(),
    val visiblePlaces: List<Place> = emptyList(),
    val clusters: List<PlaceCluster> = emptyList(),
    val selectedPlace: Place? = null,
    val error: String? = null,
    val zoomCommand: ZoomCommand? = null
)

data class ZoomCommand(
    val target: LatLng,
    val zoom: Float
)
