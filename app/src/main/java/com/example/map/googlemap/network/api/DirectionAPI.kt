package com.example.map.googlemap.network.api

import com.example.map.googlemap.network.response.DirectionResponse
import com.example.map.googlemap.utils.DIRECTIONS
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionAPI {

    @GET(DIRECTIONS)
    fun getDrivingCourse(
        @Query("origin") origin: String,
        @Query("destination") destination: String
    ) : Single<DirectionResponse>
}