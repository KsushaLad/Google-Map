package com.example.map.googlemap.network.api

import com.example.map.googlemap.network.response.GeocodeResponse
import com.example.map.googlemap.network.response.PlaceResponse
import com.example.map.googlemap.network.response.ReverseGeocodeResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query


interface GeocodeAPI {
    @GET("geocode/json")
    fun getLocationUseAddress(@Query("address") address: String): Single<GeocodeResponse>

    @GET("geocode/json")
    fun getLocationUseLatLng(@Query("latlng") latLng: String): Single<ReverseGeocodeResponse>

    @GET("geocode/json")
    fun getPlace(@Query("address") address: String) : Single<PlaceResponse>

}