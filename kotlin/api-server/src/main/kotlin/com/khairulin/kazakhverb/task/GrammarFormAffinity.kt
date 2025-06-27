package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm

enum class GrammarFormAffinity {
    unspecified,
    matchRequired,  // use the same person+number in both clauses
    mismatchRequired,  // use different person in both clauses
    ;

    fun getRandomGrammarFormPair(): Pair<GrammarForm, GrammarForm> {
        return when (this) {
            matchRequired -> {
                val form = GrammarForm.getMainRandom()
                Pair(form, form)
            }
            mismatchRequired -> {
                val first = GrammarForm.getMainRandom()
                var second = first
                for (iter in 1..10) {
                    second = GrammarForm.kMainForms.random()
                    if (second.person.personPosition != first.person.personPosition) {
                        break
                    }
                }
                if (first.person == second.person) {
                    throw IllegalStateException("failed to generate grammar forms with different persons")
                }
                Pair(first, second)
            }
            else -> {
                Pair(GrammarForm.getMainRandom(), GrammarForm.getMainRandom())
            }
        }
    }
}