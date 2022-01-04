package com.example.map.googlemap.ext

import com.google.android.gms.maps.model.LatLng

fun LatLng.toParam(): String {
    val lat: Double = latitude
    val lng: Double = longitude
    return StringBuilder(60).append(lat).append(",").append(lng).toString()
}