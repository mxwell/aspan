package com.khairulin.kazakhverb.latex

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.Phrasal

typealias VerbBuilderCall = (GrammarForm) -> Phrasal

class FormGenerator(val usePossPronoun: Boolean = false) {
    fun generate(builderCall: VerbBuilderCall): List<Pair<String, Phrasal>> {
        val result = mutableListOf<Pair<String, Phrasal>>()
        for (form in GrammarForm.entries) {
            val pronoun = if (usePossPronoun) {
                form.poss
            } else {
                form.pronoun
            }
            result.add(Pair(pronoun, builderCall(form)))
        }
        return result
    }
}