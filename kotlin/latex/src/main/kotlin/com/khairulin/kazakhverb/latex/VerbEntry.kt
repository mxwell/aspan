package com.khairulin.kazakhverb.latex

import com.khairulin.kazakhverb.grammar.VerbBuilder

data class VerbEntry(
    val verbDictForm: String,
    val forceExceptional: Boolean = false
) {
    fun builder() = VerbBuilder(verbDictForm, forceExceptional)
}
