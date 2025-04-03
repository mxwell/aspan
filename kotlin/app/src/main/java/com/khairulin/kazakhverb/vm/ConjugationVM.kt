package com.khairulin.kazakhverb.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khairulin.kazakhverb.config.ProfilePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConjugationVM : ViewModel() {
    private val tenseConfig = MutableStateFlow(ProfilePreferences.loadTenseConfig())
    private val formConfig = MutableStateFlow(ProfilePreferences.loadFormConfig())

    val tenseConfigFlow = tenseConfig.asStateFlow()
    val formConfigFlow = formConfig.asStateFlow()

    fun toggleTenseSetting(index: Int) = viewModelScope.launch {
        val updated = tenseConfig.value.toggleAt(index)
        ProfilePreferences.storeTenseConfig(updated)
        tenseConfig.value = updated
    }

    fun toggleFormSetting(index: Int) = viewModelScope.launch {
        val updated = formConfig.value.toggleAt(index)
        ProfilePreferences.storeFormConfig(updated)
        formConfig.value = updated
    }
}