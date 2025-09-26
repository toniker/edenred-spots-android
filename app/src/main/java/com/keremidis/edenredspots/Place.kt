package com.keremidis.edenredspots

import com.google.android.gms.maps.model.LatLng

data class Place(
    val id: String,
    val location: LatLng,
    val address: String,
    val name: String,
    val brandName: String,
)
