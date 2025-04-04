package com.khairulin.kazakhverb.verbdb

data class TrieResult(
    val words: List<String>,
    val suggestions: List<String>,
) {
    companion object {
        private val kEmpty = TrieResult(emptyList(), emptyList())

        fun empty() = kEmpty
    }
}
