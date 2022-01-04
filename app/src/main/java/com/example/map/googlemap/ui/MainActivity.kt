package com.example.map.googlemap.ui

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.example.map.googlemap.R
import com.example.map.googlemap.base.ui.BaseActivity
import com.example.map.googlemap.data.source.enums.SearchType
import com.example.map.googlemap.data.source.vo.DirectionVO
import com.example.map.googlemap.databinding.MainActivityBinding
import com.example.map.googlemap.ext.dip
import com.example.map.googlemap.ext.enableTransparentStatusBar
import com.example.map.googlemap.ext.nonNull
import com.example.map.googlemap.network.NetworkState
import com.example.map.googlemap.network.response.DirectionResponse
import com.example.map.googlemap.ui.dialog.SearchPlaceDialog
import com.example.map.googlemap.ui.dialog.SelectPlaceBottomDialog
import com.example.map.googlemap.utils.PolylineEncoding
import com.example.map.googlemap.utils.SimpleAnimator
import com.example.map.googlemap.utils.getCarBitmap
import com.example.map.googlemap.vm.MapViewModel
import com.tedpark.tedpermission.rx2.TedRx2Permission
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.search_place_dialog.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<MainActivityBinding>(R.layout.main_activity),
    OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener,
    GoogleMap.OnMapLongClickListener {

    private lateinit var googleMap: GoogleMap
    private lateinit var locationCallback: LocationCallback
    private val fusedLocationProviderClient by lazy { FusedLocationProviderClient(this) }
    private val mapViewModel by viewModel<MapViewModel>()
    private val selectBottomDialog by lazy { SelectPlaceBottomDialog.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initMap()
        binding.mapVM = mapViewModel
        mapViewModel.run {

            liveGeocodeState.observe(this@MainActivity, Observer { //состояние геокодирования в реальном времени
                when (it) {
                    is NetworkState.Init -> hideLoadingPopup()
                    is NetworkState.Loading -> showLoadingPopup()
                    is NetworkState.Success -> it.item.toString()
                }
            })

            liveStartLocationVO.observe(this@MainActivity, Observer { //начало пути
                if (selectBottomDialog.isAdded) {
                    selectBottomDialog.onCloseClick()
                }
                showToast(getString(R.string.toast_complete_departure))
                checkToReadyDriving(liveStartLocationVO.value, liveDestinationLocationVO.value)
            })

            liveDestinationLocationVO.observe(this@MainActivity, Observer { //конец пути
                if (selectBottomDialog.isAdded) {
                    selectBottomDialog.onCloseClick()
                }
                showToast(getString(R.string.toast_complete_destination))
                checkToReadyDriving(liveStartLocationVO.value, liveDestinationLocationVO.value)
            })

            liveDirectionState.observe(this@MainActivity, Observer { //прокладывание пути между двумя точками
                if (!::googleMap.isInitialized) return@Observer
                when (it) {
                    is NetworkState.Init -> hideLoadingPopup() //скрытие загрузки всплывающего окна
                    is NetworkState.Loading -> showLoadingPopup() //показание загрузки всплывающего окна
                    is NetworkState.Success -> {
                        val directionVO: MutableList<DirectionVO> = getDirectionsVO(it.item.routes)
                        saveDirectionInfo(directionVO)
                        val polylines = directionVO.flatMap { it.latLng }
                        if (polylines.isNotEmpty()) {
                            drawOverViewPolyline(polylines)
                            addStartEndMarker(polylines[0], polylines[polylines.size - 1])
                            startDrivingAnim(liveDirectionVO.value, polylines[0])
                        } else {
                            showToast(getString(R.string.toast_no_driving_route))
                        }
                    }
                }
            })

            liveIsDrivingStarted.nonNull().observe(this@MainActivity, Observer { //начало движения
                if (!it) {
                    clearMap()
                }
            })

            liveSearchType.observe(this@MainActivity, Observer { //тип поиска в реальном времени
                it?.let {
                    val searchType: SearchType = when (it) {
                        SearchType.SOURCE -> SearchType.SOURCE
                        SearchType.DESTINATION -> SearchType.DESTINATION
                    }
                    SearchPlaceDialog.getInstance(searchType,
                        onPlaceClickListener = { locationVO ->
                            when (searchType) {
                                SearchType.SOURCE -> mapViewModel.setDeparture(locationVO)
                                SearchType.DESTINATION -> mapViewModel.setDestination(locationVO)
                            }
                            addOneMarker(locationVO.latLng)
                            moveCamera(locationVO.latLng)
                        })
                        .show(supportFragmentManager, "")
                }
            })
        }
    }

    private fun addStartEndMarker(departure: LatLng, destination: LatLng): Pair<Marker, Marker> { //добавление маркера в начало и конец
        return googleMap.addMarker(MarkerOptions().position(departure)).apply {
            title = getString(R.string.departure)
            snippet = getString(R.string.marker_snippet_departure)
            setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        } to googleMap.addMarker(MarkerOptions().position(destination)).apply {
            title = getString(R.string.destination)
            snippet = getString(R.string.marker_snippet_destination)
            setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        }
    }

    private fun startDrivingAnim(directionsVO: List<DirectionVO>?, latLng: LatLng) { //начало анимации движения
        directionsVO?.let {
            if (it.isNotEmpty()) {
                mapViewModel.carCurrLatLng = latLng
                mapViewModel.carMarker = addCarMarker(latLng)
            }
            updateCarMarker(it)
        } ?: run {
            mapViewModel.liveIsDrivingStarted.value = false
        }
    }

    private fun updateCarMarker(directionsVO: List<DirectionVO>) { //обновление маркера автомобиля
        val allLatLngs = directionsVO.flatMap { it.latLng }
        val allLatLngLength = allLatLngs.size
        if (allLatLngLength == 1) {
            if (mapViewModel.carCurrLatLng == allLatLngs[0]) {
                showToast(getString(R.string.toast_no_route_same_departure_destination))
                return
            }
        }
        mapViewModel.liveIsDrivingStarted.value = true
        compositeDisposable.add(
            mapViewModel.observableDrive
                .take(allLatLngLength.toLong())
                .takeWhile { mapViewModel.liveIsDrivingStarted.value == true }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { cnt ->
                    if (mapViewModel.carMarker != null) {
                        if (mapViewModel.carPreviousLatLng == null) {
                            mapViewModel.carPreviousLatLng = mapViewModel.carCurrLatLng
                            moveCamera(mapViewModel.carCurrLatLng, mapViewModel.zoom)
                        } else {
                            SimpleAnimator.carStartAnim().apply {
                                mapViewModel.carPreviousLatLng = mapViewModel.carCurrLatLng
                                mapViewModel.carCurrLatLng = allLatLngs[cnt.toInt()]
                                addUpdateListener { animator ->
                                    if (mapViewModel.carCurrLatLng != null && mapViewModel.carPreviousLatLng != null) {
                                        val factor = animator.animatedFraction.toDouble()
                                        val nextLocation = LatLng(
                                            factor * mapViewModel.carCurrLatLng!!.latitude + (1 - factor) * mapViewModel.carPreviousLatLng!!.latitude,
                                            factor * mapViewModel.carCurrLatLng!!.longitude + (1 - factor) * mapViewModel.carPreviousLatLng!!.longitude
                                        )
                                        moveCamera(nextLocation, mapViewModel.zoom)
                                        mapViewModel.carMarker?.position = nextLocation
                                    }
                                }
                                if (cnt.toInt() == allLatLngLength - 1) {
                                    Handler().postDelayed({
                                        mapViewModel.stopDriving()
                                        showToast(getString(R.string.toast_finish_driving))
                                    }, 500)
                                }
                            }.start()
                        }
                    }
                })
    }

    private fun clearMap() { //очищение карты
        mapViewModel.liveAllArriveTime.value = ""
        mapViewModel.carPreviousLatLng = null
        if (::googleMap.isInitialized) {
            googleMap.clear()
        }
    }

    private fun addCarMarker(latLng: LatLng): Marker? { //добавление маркера автомобиля
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
            getCarBitmap(
                resources,
                R.drawable.icon_car_maker,
                dip(28),
                dip(28)
            )
        )
        return googleMap.addMarker(MarkerOptions().position(latLng).icon(bitmapDescriptor))
    }

    private fun drawOverViewPolyline(routes: List<LatLng>) { //рисование линии поверх карты
        googleMap.clear()
        googleMap.addPolyline(PolylineOptions().addAll(routes)).apply {
            color = ResourcesCompat.getColor(resources, R.color.black_87, theme)
            isClickable = true
        }
    }

    private fun getDirectionsVO(routes: List<DirectionResponse.Route?>?): MutableList<DirectionVO> { //проложение маршрута
        val directionVO: MutableList<DirectionVO> = mutableListOf()
        routes?.forEach {
            it?.legs?.forEach {
                mapViewModel.liveAllArriveTime.value = it?.duration?.text
                it?.steps?.forEachIndexed { idx, it ->
                    it?.let {
                        val latLngs = PolylineEncoding.decode(it.polyline.points)
                        directionVO.add(
                            DirectionVO(
                                latLngs,
                                it.duration?.text,
                                it.distance?.text,
                                idx
                            )
                        )
                    }
                }
            }
        }
        return directionVO
    }

    private fun checkPermissions() { //проверка разрешений
        compositeDisposable.add(TedRx2Permission.with(this)
            .setDeniedCloseButtonText(getString(R.string.cancel))
            .setGotoSettingButtonText(getString(R.string.setting))
            .setDeniedTitle(getString(R.string.request_gps_permission))
            .setDeniedMessage(getString(R.string.desc_gps_permission))
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .request()
            .subscribe({
                if (it.isGranted) {
                    setLocationListener()
                    getCurrentLocation()
                } else {
                    showToast(getString(R.string.desc_gps_permission))
                }
            }) {

            })
    }

    @SuppressLint("MissingPermission")
    private fun setLocationListener() { //прослушиватель местоположения
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) { //по местоположению результат
                super.onLocationResult(locationResult)
                mapViewModel.currLatLng.let { latLng ->
                    locationResult?.let { locationResult ->
                        for (location in locationResult.locations) {
                            if (latLng == null) {
                                mapViewModel.currLatLng =
                                    LatLng(location.latitude, location.longitude)
                                moveCamera(mapViewModel.currLatLng, mapViewModel.zoom)
                                animateCamera(mapViewModel.currLatLng)
                                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                            }
                        }
                    }
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability?) { //по доступности местоположения
                super.onLocationAvailability(locationAvailability)
                mapViewModel.isAvailabilityLocation =
                    locationAvailability?.isLocationAvailable ?: false
            }
        }


        val locationRequest = LocationRequest().setInterval(10000).setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun animateCamera(latLng: LatLng?) { //анимация камеры
        latLng?.let {
            val cameraPosition = CameraPosition.Builder().target(it).zoom(16f).build()
            googleMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    cameraPosition
                )
            )
        } ?: throw NullPointerException(getString(R.string.error_no_location))
    }


    private fun moveCamera(latLng: LatLng?, zoom: Float) { //перемещении камеры
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun moveCamera(latLng: LatLng?) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() { //текущее местоположение
        mapViewModel.run {
            googleMap.setOnMyLocationButtonClickListener(this@MainActivity)
            googleMap.setOnMyLocationClickListener(this@MainActivity)
            googleMap.setOnMapLongClickListener(this@MainActivity)
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun initView() { //прозрачный StatusBar
       window.enableTransparentStatusBar()
    }

    private fun initMap() { //инициализация карты
        clearMap()
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.fcv_map) as SupportMapFragment
        mapFragment.getMapAsync(this@MainActivity)
    }

    override fun onMapReady(googleMap: GoogleMap?) { //готовность карты
        googleMap?.let {
            this.googleMap = it
            googleMap.setPadding(dip(20), dip(20), dip(20), dip(20))
            checkPermissions()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }


    override fun onMyLocationClick(location: Location) {
    }


    override fun onMapLongClick(latLng: LatLng?) { //долгий клик по карте
        if (mapViewModel.liveIsDrivingStarted.value == true) return
        latLng?.let {
            addOneMarker(it)
            selectBottomDialog.show(supportFragmentManager, selectBottomDialog.tag)
            selectBottomDialog.searchLocation(it)
        } ?: throw NullPointerException(getString(R.string.error_no_location))
    }

    private fun addOneMarker(latLng: LatLng) { //добавление одного маркера
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng).flat(true))
    }
}