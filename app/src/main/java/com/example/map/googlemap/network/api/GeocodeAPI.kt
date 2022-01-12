package com.example.map.googlemap.network.api

import com.example.map.googlemap.network.response.GeocodeResponse
import com.example.map.googlemap.network.response.PlaceResponse
import com.example.map.googlemap.network.response.ReverseGeocodeResponse
import com.example.map.googlemap.utils.GEOCODE
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodeAPI {
    @GET(GEOCODE)
    fun getLocationUseAddress(@Query("address") address: String): Single<GeocodeResponse>

    @GET(GEOCODE)
    fun getLocationUseLatLng(@Query("latlng") latLng: String): Single<ReverseGeocodeResponse>

    @GET(GEOCODE)
    fun getPlace(@Query("address") address: String) : Single<PlaceResponse>
}