package com.keremidis.edenredspots

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keremidis.edenredspots.ui.theme.EdenredSpotsTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EdenRedSpotsApp : android.app.Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: DataPreparationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EdenredSpotsTheme {
                DataPreparationScreen(viewModel) {
                    // Navigate to MapActivity
                    val intent = Intent(this@MainActivity, MapActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}

@Composable
private fun DataPreparationScreen(
    viewModel: DataPreparationViewModel, onNavigateToMap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(), onResult = { isGranted ->
            hasLocationPermission = isGranted
            viewModel.onPermissionResult(isGranted)
        })

    LaunchedEffect(Unit) {
        viewModel.loadPlaces()
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Edenred Spots",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.dataLoadingState) {
                DataLoadingState.Loading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading places data...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }

                DataLoadingState.Loaded -> {
                    Text(
                        text = "✓ Data loaded successfully",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                DataLoadingState.Error -> {
                    Text(
                        text = "✗ Failed to load data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!hasLocationPermission) {
                Text(
                    text = "Location permission is required to show nearby places",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Grant Location Permission", style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                Text(
                    text = "✓ Location permission granted",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNavigateToMap,
                enabled = uiState.canProceed && hasLocationPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Go", style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@HiltViewModel
class DataPreparationViewModel @Inject constructor(
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPreparationUiState())
    val uiState: StateFlow<DataPreparationUiState> = _uiState.asStateFlow()

    fun loadPlaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(dataLoadingState = DataLoadingState.Loading)
            try {
                placesRepository.loadPlaces()
                _uiState.value = _uiState.value.copy(
                    dataLoadingState = DataLoadingState.Loaded, canProceed = true
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    dataLoadingState = DataLoadingState.Error, canProceed = false
                )
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = isGranted)
    }
}

data class DataPreparationUiState(
    val dataLoadingState: DataLoadingState = DataLoadingState.Loading,
    val hasLocationPermission: Boolean = false,
    val canProceed: Boolean = false
)

enum class DataLoadingState {
    Loading, Loaded, Error
}