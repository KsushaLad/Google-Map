package com.example.map.googlemap.data.source

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.example.map.googlemap.network.NetworkState
import com.example.map.googlemap.network.response.PlaceResponse
import io.reactivex.disposables.CompositeDisposable

class SearchPlaceDataSourceFactory(
    private val geocodeRepository: GeocodeRepository,
    private val keyword: String,
    private val livePlaceState: MutableLiveData<NetworkState<PlaceResponse>>,
    private val compositeDisposable: CompositeDisposable
) : DataSource.Factory<String, PlaceResponse.Result>() {

    override fun create(): DataSource<String, PlaceResponse.Result> {
        return SearchPlaceDataSource(geocodeRepository, keyword, livePlaceState, compositeDisposable)
    }
}
