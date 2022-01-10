package com.example.map.googlemap.extensions

import android.annotation.SuppressLint
import android.view.View
import android.widget.EditText
import androidx.databinding.BindingAdapter
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.example.map.googlemap.data.source.SearchPlaceDataSourceFactory
import com.example.map.googlemap.network.response.PlaceResponse
import com.jakewharton.rxbinding3.view.clicks
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
@BindingAdapter(value = ["onShortBlockClick"])
fun View.setOnShortBlockClick(listener: View.OnClickListener) {
    clicks().throttleFirst(250L, TimeUnit.MILLISECONDS)
        .subscribe {
            listener.onClick(this)
        }
}

@BindingAdapter(value = ["enabled"])
fun View.setEnabled(enabled: Boolean) {
    isEnabled = enabled
}
