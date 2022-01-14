package com.example.map.googlemap.base.ui

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseViewModel : ViewModel() {

    //TODO есл и ты уже сделала такую реализацию с базовой вьюмоделью, то лучше уже сделать базовые методы на добавление
    //    fun bindDisposable(disposable: Disposable) {
    //        disposables + disposable
    //    }
    //    fun autoDisposable(disposableFun: () -> Disposable) {
    //        disposables + disposableFun()
    //    }

    //TODO И лучше сделать compositeDisposable как lazy переменную
    protected val compositeDisposable = CompositeDisposable()

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }
}