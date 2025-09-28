package com.keremidis.edenredspots

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class Place(
    val id: String,
    val location: LatLng,
    val address: String,
    val name: String,
    val brandName: String,
) : ClusterItem {
    override fun getPosition(): LatLng = location
    override fun getTitle(): String = name
    override fun getSnippet(): String = address
    override fun getZIndex(): Float? = null // Remove expensive random calculation

    companion object {
        val DEFAULT_LAT_LNG = LatLng(40.6401, 22.9444) // Thessaloniki, Greece
    }
}
