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
import com.khairulin.kazakhverb.grammar.SentenceType

class ConjugationVM : ViewModel() {
    private val TAG = "ConjugationVM"

    var state by mutableStateOf(ViewModelState.awaitingInput)
        private set
    var lastEntered by mutableStateOf("")
        private set
    var selectedSentenceTypeIndex by mutableStateOf(0)
        private set
    var optionalExceptional by mutableStateOf(true)
        private set
    var conjugationTypeIndex by mutableStateOf(0)
        private set
    var loadedVerb by mutableStateOf("")
        private set
    var contAuxVerbIndex by mutableStateOf(0)
        private set

    private val tenses = MutableStateFlow(listOf<TenseInfo>())

    private val suggestions = MutableStateFlow(listOf<String>())
    private val tenseConfig = MutableStateFlow(ProfilePreferences.loadTenseConfig())
    private val formConfig = MutableStateFlow(ProfilePreferences.loadFormConfig())

    val suggestionsFlow = suggestions.asStateFlow()
    val tensesFlow = tenses.asStateFlow()

    val tenseConfigFlow = tenseConfig.asStateFlow()
    val formConfigFlow = formConfig.asStateFlow()

    private val trie = TrieLoader.getTrie()
    private val generator = Generator()
    private var appliedSuggestion: String? = null

    fun toggleTenseSetting(index: Int) = viewModelScope.launch {
        val updated = tenseConfig.value.toggleAt(index)
        ProfilePreferences.storeTenseConfig(updated)
        tenseConfig.value = updated
        reload(loadedVerb)
    }

    fun toggleFormSetting(index: Int) = viewModelScope.launch {
        val updated = formConfig.value.toggleAt(index)
        ProfilePreferences.storeFormConfig(updated)
        formConfig.value = updated
        reload(loadedVerb)
    }

    private fun putToState(state: ViewModelState) {
        this.state = state
    }

    private fun putToLoadingForms(verb: String): Boolean {
        if (state == ViewModelState.loadingForms) {
            return false
        }
        this.loadedVerb = verb
        putToState(ViewModelState.loadingForms)
        return true
    }

    private fun putToLoadedForms(tenses: List<TenseInfo>, optExcept: Boolean) {
        this.tenses.value = tenses
        this.optionalExceptional = optExcept
        putToState(ViewModelState.loadedForms)
    }

    private fun putToNotFound() {
        putToState(ViewModelState.notFound)
    }

    private fun loadForms(verb: String, sentenceType: SentenceType, contAux: ContinuousAuxVerb) {
        val aFormConfig = formConfig.value
        val conjugationType = ConjugationType.entries[conjugationTypeIndex]

        val tenses = mutableListOf<TenseInfo>()

        TenseId.entries.forEachIndexed { index, tenseId ->
            if (tenseConfig.value.settings[index].on) {
                val tense = generator.generateTense(
                    tenseId,
                    aFormConfig,
                    verb,
                    sentenceType,
                    contAux,
                    conjugationType,
                )
                if (tense == null) {
                    putToNotFound()
                    return
                }
                tenses.add(tense)
            }
        }

        putToLoadedForms(tenses, generator.isOptExcept(verb))
    }

    private fun reload(newVerb: String) {
        if (newVerb.length < 2) {
            Log.e(TAG, "reload: too short input")
            return
        }
        val sentenceType = SentenceType.entries[selectedSentenceTypeIndex]
        val contAux = ContinuousAuxVerb.entries[contAuxVerbIndex]

        if (!putToLoadingForms(newVerb)) {
            Log.e(TAG, "reload: already loading")
            return
        }

        loadForms(newVerb, sentenceType, contAux)

        this.suggestions.value = emptyList()
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

    fun applySuggestion(suggestion: String) {
        appliedSuggestion = suggestion
        lastEntered = suggestion
        this.suggestions.value = emptyList()
        reload(suggestion)
    }

    fun onSubmit() = viewModelScope.launch {
        Log.i(TAG, "onSubmit called")
        val newVerb = lastEntered
        reload(newVerb)
    }

    fun onSentenceTypeChange(newIndex: Int) {
        if (newIndex !in 0 until SentenceType.entries.size) {
            Log.e(TAG, "onSentenceTypeChange: bad index ${newIndex}")
            return
        }
        selectedSentenceTypeIndex = newIndex
        Log.i(TAG, "onSentenceTypeChange: ${selectedSentenceTypeIndex}")
        reload(loadedVerb)
    }

    fun onConjugationTypeChange(newIndex: Int) {
        if (newIndex !in 0 until ConjugationType.entries.size) {
            Log.e(TAG, "onConjugationTypeChange: bad index ${newIndex}")
            return
        }
        conjugationTypeIndex = newIndex
        Log.i(TAG, "onConjugationTypeChange: ${conjugationTypeIndex}")
        reload(loadedVerb)
    }

    fun onContAuxVerbChange(newIndex: Int) {
        if (newIndex !in 0 until ContinuousAuxVerb.entries.size) {
            Log.e(TAG, "onContAuxVerbChange: bad index ${newIndex}")
            return
        }
        contAuxVerbIndex = newIndex
        Log.i(TAG, "onContAuxVerbChange: ${contAuxVerbIndex}")
        reload(loadedVerb)
    }
}