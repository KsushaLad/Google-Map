package com.example.map.googlemap.di

import com.example.map.googlemap.network.api.DirectionAPI
import com.example.map.googlemap.network.api.GeocodeAPI
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

const val DI_API_NO_AUTH = "DI_API_NO_AUTH"

val apiModule = module {
    single(named(DI_API_NO_AUTH)) { (get(named(DI_RETROFIT_NO_AUTH)) as Retrofit).create(GeocodeAPI::class.java) }
    single(named(DI_API_NO_AUTH)) { (get(named(DI_RETROFIT_NO_AUTH)) as Retrofit).create(DirectionAPI::class.java) }
}
