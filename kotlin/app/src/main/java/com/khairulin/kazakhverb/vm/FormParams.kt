package com.khairulin.kazakhverb.vm

import com.khairulin.kazakhverb.grammar.GrammarNumber
import com.khairulin.kazakhverb.grammar.GrammarPerson

data class FormParams(
    val grammarPerson: GrammarPerson,
    val grammarNumber: GrammarNumber,
)
