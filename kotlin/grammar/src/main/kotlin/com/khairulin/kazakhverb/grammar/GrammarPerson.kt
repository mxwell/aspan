package com.khairulin.kazakhverb.grammar

enum class GrammarPerson(val personPosition: Int) {
    First(1),
    Second(2),
    SecondPolite(2),
    Third(3),
    ;

    companion object {
        fun ofIndex(index: Int): GrammarPerson {
            return entries[index]
        }
    }
}