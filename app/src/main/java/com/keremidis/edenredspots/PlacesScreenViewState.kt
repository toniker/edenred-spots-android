package com.keremidis.edenredspots

import com.google.android.gms.maps.model.LatLngBounds

sealed class PlacesScreenViewState {
    data object Loading : PlacesScreenViewState()

    data class PlaceList(
        val places: List<Place>,
        val boundingBox: LatLngBounds,
    ) : PlacesScreenViewState()
}
