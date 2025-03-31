package org.example

data class Phrasal(
    val parts: List<PhrasalPart>,
    val raw: String,
    val forbidden: Boolean
)
