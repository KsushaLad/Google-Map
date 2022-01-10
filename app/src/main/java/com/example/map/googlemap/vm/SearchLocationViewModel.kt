package com.example.map.googlemap.vm


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.map.googlemap.base.ui.BaseViewModel
import com.example.map.googlemap.data.source.GeocodeRepository
import com.example.map.googlemap.data.source.LocalSearchPlaceRepository
import com.example.map.googlemap.data.source.SearchPlaceDataSourceFactory
import com.example.map.googlemap.data.source.vo.LocationVO
import com.example.map.googlemap.network.NetworkState
import com.example.map.googlemap.network.response.PlaceResponse

class SearchLocationViewModel(
    private val geocodeRepository: GeocodeRepository,
    private val localSearchPlaceRepository: LocalSearchPlaceRepository
) : BaseViewModel() {

    var liveSearchItems = MutableLiveData<LiveData<PagedList<PlaceResponse.Result>>>() //элементы поиска
    val liveLocalLocations = MutableLiveData<List<LocationVO>>() //локальное местоположение
    private val _livePlaceState = MutableLiveData<NetworkState<PlaceResponse>>()
    val livePlaceState: LiveData<NetworkState<PlaceResponse>> get() = _livePlaceState //состояние места
    val liveKeyword = MutableLiveData<String>() //ключевое слово
    private val _liveIsResultZero = MutableLiveData<Boolean>()
    val liveIsResultZero: LiveData<Boolean> get() = _liveIsResultZero

    fun onSearchClick() {
        _liveIsResultZero.value = false
        liveSearchItems.value = LivePagedListBuilder(
            SearchPlaceDataSourceFactory(
                geocodeRepository,
                liveKeyword.value  ?: error("empty keyword"),
                _livePlaceState,
                compositeDisposable
            ), 10
        ).setBoundaryCallback(object : PagedList.BoundaryCallback<PlaceResponse.Result>() {
            override fun onZeroItemsLoaded() {
                super.onZeroItemsLoaded()
                _liveIsResultZero.value = true
            }
        }).build()
    }

    fun saveLocations(locationVO: LocationVO) {
        localSearchPlaceRepository.saveLocationVOList(locationVO) {
        }
    }

    fun loadLocalLocations() { //загрузка местоположений
        liveLocalLocations.value = localSearchPlaceRepository.loadLocationVOList()?.reversed()
    }

    fun clearLocalLocations() { //очищение
        localSearchPlaceRepository.clearLocationVOList {
            liveLocalLocations.value = emptyList()
        }
    }
}
