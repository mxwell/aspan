package com.khairulin.kazakhverb.grammar.tester

import kotlinx.serialization.Serializable

@Serializable
data class NounFormsRow(
    val pos: String,
    val base: String,
    val forms: List<NounForm>,
)
