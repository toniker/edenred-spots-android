package com.keremidis.edenredspots

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonPlace(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Phone") val phone: String? = null,
    @SerialName("ZipCode") val zipCode: String? = null,
    @SerialName("Address") val address: String,
    @SerialName("Address2") val address2: String? = null,
    @SerialName("CityName") val cityName: String? = null,
    @SerialName("AffiliateCode") val affiliateCode: String? = null,
    @SerialName("Lg") val longitude: Double, // JSON uses "Lg" for longitude
    @SerialName("Lt") val latitude: Double,  // JSON uses "Lt" for latitude
    @SerialName("Photo") val photo: String? = null,
    @SerialName("Fields") val fields: List<JsonField>? = null,
    @SerialName("AditionalAtributes") val additionalAttributes: JsonAdditionalAttributes? = null,
    val placeID: String? = null, // These seem to follow Kotlin naming conventions already
    val brandID: String? = null,
    val brandName: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonField(
    @SerialName("Champ_SQL") val champSql: String,
    @SerialName("Nom_colonne") val nomColonne: String,
    @SerialName("Position") val position: Int,
    @SerialName("Value") val value: String,
    @SerialName("SqlFields") val sqlFields: List<String>,
    @SerialName("Visible") val visible: Boolean
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class JsonAdditionalAttributes(
    @SerialName("Container") val container: Map<String, String>? = emptyMap() // Assuming it's a map or can be empty
)
