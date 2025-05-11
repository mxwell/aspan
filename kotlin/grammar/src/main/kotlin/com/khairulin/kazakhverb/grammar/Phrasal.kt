package com.khairulin.kazakhverb.grammar

data class Phrasal(
    val parts: List<PhrasalPart>,
    val raw: String,
    val forbidden: Boolean,
    val alternative: Phrasal? = null
)
