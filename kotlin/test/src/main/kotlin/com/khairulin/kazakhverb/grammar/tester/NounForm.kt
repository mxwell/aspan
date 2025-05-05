package com.khairulin.kazakhverb.grammar.tester

import kotlinx.serialization.Serializable

@Serializable
data class NounForm(
    val form: String,
    val weight: Double,
    val number: Int? = null,
    val septik: Int? = null,
    val possPerson: Int? = null,
    val possNumber: Int? = null,
    val wordgen: String? = null,
)
