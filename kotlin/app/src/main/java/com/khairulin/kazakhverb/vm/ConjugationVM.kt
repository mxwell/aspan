package com.khairulin.kazakhverb.vm

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khairulin.kazakhverb.config.ProfilePreferences
import com.khairulin.kazakhverb.verbdb.TrieLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.SentenceType

class ConjugationVM : ViewModel() {
    private val TAG = "ConjugationVM"

    private val state = MutableStateFlow(ViewModelState.awaitingInput)

    var lastEntered by mutableStateOf("ке")
        private set
    var selectedSentenceTypeIndex by mutableStateOf(0)
        private set

    private val suggestions = MutableStateFlow(listOf<String>())
    private val tenseConfig = MutableStateFlow(ProfilePreferences.loadTenseConfig())
    private val formConfig = MutableStateFlow(ProfilePreferences.loadFormConfig())

    val stateFlow = state.asStateFlow()
    val suggestionsFlow = suggestions.asStateFlow()
    val tenseConfigFlow = tenseConfig.asStateFlow()
    val formConfigFlow = formConfig.asStateFlow()

    private val trie = TrieLoader.getTrie()
    private var appliedSuggestion: String? = null

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

    private fun updateSuggestions(newSuggestions: List<String>) {
        suggestions.value = newSuggestions
    }

    fun onVerbChange(newVerb: String) {
        lastEntered = newVerb
        Log.i(TAG, "lastEntered: ${lastEntered}")

        viewModelScope.launch {
            val atrie = trie
            if (atrie == null) {
                return@launch
            }
            if (newVerb == appliedSuggestion) {
                return@launch
            }
            if (newVerb.isEmpty()) {
                suggestions.value = emptyList<String>()
                return@launch
            }
            val result = atrie.traverse(newVerb)
            Log.i(TAG, "traverse result: ${result.words.size} words, ${result.suggestions.size} suggs")
            val newSuggestions = result.words + result.suggestions
            updateSuggestions(newSuggestions)
        }
    }

    fun onSentenceTypeChange(newIndex: Int) {
        if (newIndex !in 0 until SentenceType.entries.size) {
            Log.e(TAG, "onSentenceTypeChange: bad index ${newIndex}")
            return
        }
        selectedSentenceTypeIndex = newIndex
        Log.i(TAG, "onSentenceTypeChange: ${selectedSentenceTypeIndex}")
    }

    fun onSubmit() = viewModelScope.launch {
        Log.i(TAG, "onSubmit called")
        // TODO
    }

    fun onSuggestionClick(suggestion: String) = viewModelScope.launch {
        Log.i(TAG, "onSuggestionClick called: ${suggestion}")
        // TODO
    }
}