package com.khairulin.kazakhverb.verbdb

data class TrieNode(
    val words: List<Int>,
    val suggestions: List<Int>,
    val children: Map<Char, Int>,
)