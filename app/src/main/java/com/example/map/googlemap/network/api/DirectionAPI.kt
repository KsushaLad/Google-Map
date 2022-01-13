package com.example.map.googlemap.network.api

import com.example.map.googlemap.network.response.DirectionResponse
import com.example.map.googlemap.utils.DESTINATION
import com.example.map.googlemap.utils.DIRECTIONS
import com.example.map.googlemap.utils.ORIGIN
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionAPI {

    @GET(DIRECTIONS)
    fun getDrivingCourse(
        @Query(ORIGIN) origin: String,
        @Query(DESTINATION) destination: String
    ) : Single<DirectionResponse>
}
