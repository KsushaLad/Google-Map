package com.example.map.googlemap.ext

import com.google.gson.Gson

inline fun Any.toJsonString(): String = Gson().toJson(this)