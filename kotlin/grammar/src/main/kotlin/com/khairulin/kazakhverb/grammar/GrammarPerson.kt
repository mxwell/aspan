package com.khairulin.kazakhverb.grammar

enum class GrammarPerson {
    First,
    Second,
    SecondPolite,
    Third,
    ;

    companion object {
        fun ofIndex(index: Int): GrammarPerson {
            return entries[index]
        }
    }
}