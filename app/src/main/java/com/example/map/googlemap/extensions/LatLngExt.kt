package com.example.map.googlemap.extensions

import com.google.android.gms.maps.model.LatLng

//TODO тут не нужны временные переменные типа lat...
fun LatLng.toParam(): String {
    val lat: Double = latitude
    val lng: Double = longitude
    return StringBuilder(60).append(lat).append(",").append(lng).toString()
}