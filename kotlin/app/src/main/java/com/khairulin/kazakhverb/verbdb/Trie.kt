package com.khairulin.kazakhverb.verbdb

class Trie(val suggestions: List<String>, val nodes: List<TrieNode>) {
    fun traverse(path: String): TrieResult {
        var node = nodes[0]
        for (char in path) {
            val nextNode = node.children[char]
            if (nextNode == null) {
                return TrieResult.empty()
            }
            node = nodes[nextNode]
        }
        val words = mutableListOf<String>()
        for (wordId in node.words) {
            words.add(suggestions[wordId])
        }
        val curSuggestions = mutableListOf<String>()
        for (suggestionId in node.suggestions) {
            curSuggestions.add(suggestions[suggestionId])
        }
        return TrieResult(words, curSuggestions)
    }
}