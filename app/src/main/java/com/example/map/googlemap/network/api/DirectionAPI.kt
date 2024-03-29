package com.example.map.googlemap.network.api

import com.example.map.googlemap.network.response.DirectionResponse
import com.example.map.googlemap.utils.DESTINATION
import com.example.map.googlemap.utils.ORIGIN
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query


interface DirectionAPI {

    // TODO companion object лучше закидывать в самый низ класса
    companion object {
        const val directions = "directions/json"
    }

    @GET(directions)
    fun getDrivingCourse(
        @Query(ORIGIN) origin: String,
        @Query(DESTINATION) destination: String
    ): Single<DirectionResponse>
}