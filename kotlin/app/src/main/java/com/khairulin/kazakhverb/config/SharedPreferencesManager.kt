package com.khairulin.kazakhverb.config

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesManager {
    val kPrefName = "KazakhVerbPreferences"
    lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(kPrefName, Context.MODE_PRIVATE)
    }

    private fun storeValue(callback: (SharedPreferences.Editor) -> SharedPreferences.Editor) {
        callback(sharedPreferences.edit()).apply()
    }

    fun storeString(key: String, value: String) {
        storeValue {
            it.putString(key, value)
        }
    }

    fun loadString(key: String): String {
        return sharedPreferences.getString(key, "") ?: ""
    }
}