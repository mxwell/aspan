package com.khairulin.kazakhverb.grammar

enum class GrammarNumber {
    Singular,
    Plural,
    ;

    companion object {
        fun ofIndex(index: Int): GrammarNumber {
            return entries[index]
        }
    }
}