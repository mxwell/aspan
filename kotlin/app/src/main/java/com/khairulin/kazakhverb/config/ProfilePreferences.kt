package com.khairulin.kazakhverb.config

import android.util.Log

object ProfilePreferences {
    private val TAG = "ProfilePreferences"

    private val TENSE_CONFIG = "tense_config"
    private val FORM_CONFIG = "form_config"

    fun loadTenseConfig(): ConfigSection {
        val stored = SharedPreferencesManager.loadString(TENSE_CONFIG)
        try {
            Log.i(TAG, "loaded value for ${TENSE_CONFIG}: ${stored}")
            val decoded = ConfigSection.decodeTenseConfigFromString(stored)
            return decoded
        } catch (e: Exception) {
            Log.e(TAG, "failed to parse ${TENSE_CONFIG}", e)
            return ConfigSection.makeTenseConfigDefault()
        }
    }

    fun storeTenseConfig(config: ConfigSection) {
        val encoded = config.encodeToString()
        SharedPreferencesManager.storeString(TENSE_CONFIG, encoded)
        Log.i(TAG, "stored ${TENSE_CONFIG}: ${encoded}")
    }

    fun loadFormConfig(): ConfigSection {
        val stored = SharedPreferencesManager.loadString(FORM_CONFIG)
        try {
            Log.i(TAG, "loaded value for ${FORM_CONFIG}: ${stored}")
            val decoded = ConfigSection.decodeFormConfigFromString(stored)
            return decoded
        } catch (e: Exception) {
            Log.e(TAG, "failed to parse ${FORM_CONFIG}", e)
            return ConfigSection.makeFormConfigDefault()
        }
    }

    fun storeFormConfig(config: ConfigSection) {
        val encoded = config.encodeToString()
        SharedPreferencesManager.storeString(FORM_CONFIG, encoded)
        Log.i(TAG, "stored ${FORM_CONFIG}: ${encoded}")
    }
}