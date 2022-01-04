package com.example.map.googlemap.data.source

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
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
        return SearchPlaceDataSource()
    }

    inner class SearchPlaceDataSource : PageKeyedDataSource<String, PlaceResponse.Result>() {

        override fun loadInitial(
            params: LoadInitialParams<String>,
            callback: LoadInitialCallback<String, PlaceResponse.Result>
        ) {
            compositeDisposable.add(
                geocodeRepository.getPlace(keyword)
                    .doOnSubscribe { livePlaceState.postValue(NetworkState.loading()) }
                    .doOnTerminate { livePlaceState.postValue(NetworkState.init()) }
                    .subscribe({
                        callback.onResult(it.results, "", it.nextPageToken)
                        livePlaceState.postValue(NetworkState.success(it))
                    }, {
                        livePlaceState.postValue(NetworkState.error(it))
                    })
            )
        }

        override fun loadAfter(
            params: LoadParams<String>,
            callback: LoadCallback<String, PlaceResponse.Result>
        ) {
            compositeDisposable.add(
                geocodeRepository.getPlace(keyword)
                    .doOnSubscribe { livePlaceState.postValue(NetworkState.loading()) }
                    .doOnTerminate { livePlaceState.postValue(NetworkState.init()) }
                    .subscribe({
                        callback.onResult(it.results, it.nextPageToken)
                        livePlaceState.postValue(NetworkState.success(it))
                    }, {
                        livePlaceState.postValue(NetworkState.error(it))
                    })
            )
        }

        override fun loadBefore(
            params: LoadParams<String>,
            callback: LoadCallback<String, PlaceResponse.Result>
        ) { }
    }
}
