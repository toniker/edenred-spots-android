package com.keremidis.edenredspots

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap
import com.keremidis.edenredspots.ui.theme.EdenredSpotsTheme
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EdenRedSpotsApp : Application()

class MainActivity : ComponentActivity() {
    private var isMapLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdenredSpotsTheme {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    onMapLoaded = { isMapLoaded = true }
                )
            }
        }
    }
}
