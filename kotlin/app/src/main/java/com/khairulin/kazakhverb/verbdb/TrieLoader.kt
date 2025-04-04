package com.khairulin.kazakhverb.verbdb

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class TrieLoader(private val context: Context) {
    private fun loadChars(): List<Char>? {
        try {
            context.assets.open("Chars.txt").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                return reader.readLines()
                    .filter { it.isNotEmpty() }
                    .map { it[0] }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadChars: error reading file: $e")
            return null
        }
    }

    private fun loadSuggestions(): List<String>? {
        try {
            context.assets.open("Suggestions.txt").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                return reader.readLines()
                    .filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadSuggestions: error reading file: $e")
            return null
        }
    }

    private fun decodeSuggestion(encoded: String, chars: List<Char>): String? {
        val result = mutableListOf<Char>()
        for (part in encoded.split(" ")) {
            try {
                val charId = part.toInt()
                result.add(chars[charId])
            } catch (e: Exception) {
                Log.e(TAG, "bad character id: $part")
                return null
            }
        }
        return result.joinToString("")
    }

    private fun decodeSuggestions(encoded: List<String>, chars: List<Char>): List<String>? {
        val result = mutableListOf<String>()
        for (encodedSuggestion in encoded) {
            val suggestion = decodeSuggestion(encodedSuggestion, chars)
            if (suggestion == null) {
                Log.e(TAG, "bad suggestion: $encodedSuggestion")
                return null
            }
            result.add(suggestion)
        }
        return result
    }

    private fun loadNodes(chars: List<Char>): List<TrieNode>? {
        try {
            context.assets.open("Trie.txt").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()

                val nodes = mutableListOf<TrieNode>()
                for (line in content.split("\n").filter { it.isNotEmpty() }) {
                    val parts = line.split("\t")
                    var offset = 0

                    val wordCount = try {
                        parts[offset].toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "loadTrie: bad word count ${parts[offset]}")
                        return null
                    }
                    offset += 1

                    val words = mutableListOf<Int>()
                    for (i in 0 until wordCount) {
                        try {
                            val wordId = parts[offset + i].toInt()
                            words.add(wordId)
                        } catch (e: Exception) {
                            Log.e(TAG, "loadTrie: bad word id ${parts[offset + i]}")
                            return null
                        }
                    }
                    offset += wordCount

                    val suggestionsCount = try {
                        parts[offset].toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "loadTrie: bad suggestions count ${parts[offset]}")
                        return null
                    }
                    offset += 1

                    val suggestions = mutableListOf<Int>()
                    for (i in 0 until suggestionsCount) {
                        try {
                            val suggestionId = parts[offset + i].toInt()
                            suggestions.add(suggestionId)
                        } catch (e: Exception) {
                            Log.e(TAG, "loadTrie: bad suggestion id ${parts[offset + i]}")
                            return null
                        }
                    }
                    offset += suggestionsCount

                    val childrenCount = try {
                        parts[offset].toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "loadTrie: bad children count ${parts[offset]}")
                        return null
                    }
                    offset += 1

                    val children = mutableMapOf<Char, Int>()
                    for (i in 0 until childrenCount) {
                        val charId = try {
                            parts[offset].toInt()
                        } catch (e: Exception) {
                            Log.e(TAG, "loadTrie: bad char id ${parts[offset]}")
                            return null
                        }

                        if (charId < 0 || charId >= chars.size) {
                            Log.e(TAG, "loadTrie: char id is out of range: $charId, ${chars.size}")
                            return null
                        }

                        val char = chars[charId]
                        offset += 1

                        val child = try {
                            parts[offset].toInt()
                        } catch (e: Exception) {
                            Log.e(TAG, "loadTrie: bad child id ${parts[offset]}")
                            return null
                        }
                        offset += 1

                        children[char] = child
                    }

                    val node = TrieNode(words, suggestions, children)
                    nodes.add(node)
                }
                return nodes
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadNodes: error reading file: $e")
            return null
        }
    }

    private fun load(): Trie? {
        val chars = loadChars()
        if (chars == null) {
            Log.e(TAG, "load: failed to load chars")
            return null
        }
        Log.i(TAG, "load: loaded ${chars.size} chars")

        val encodedSuggestions = loadSuggestions()
        if (encodedSuggestions == null) {
            Log.e(TAG, "load: failed to load suggestions")
            return null
        }

        val suggestions = decodeSuggestions(encodedSuggestions, chars)
        if (suggestions == null) {
            Log.e(TAG, "load: failed to decode suggestions")
            return null
        }
        Log.i(TAG, "load: loaded ${suggestions.size} suggestions")

        val nodes = loadNodes(chars)
        if (nodes == null) {
            Log.e(TAG, "load: failed to load nodes")
            return null
        }
        Log.i(TAG, "load: loaded ${nodes.size} nodes")

        return Trie(suggestions, nodes)
    }

    companion object {
        private val TAG = "TrieLoader"
        private var loadedTrie: Trie? = null

        fun loadTrie(context: Context) {
            Log.i(TAG, "Loading trie...")
            val trie = TrieLoader(context).load()
            if (trie == null) {
                Log.e(TAG, "Failed to load trie")
            } else {
                Log.i(TAG, "Trie is loaded: ${trie.suggestions.size} suggestions, ${trie.nodes.size} nodes")
            }
            loadedTrie = trie
        }

        fun getTrie(): Trie? {
            return loadedTrie
        }
    }
}