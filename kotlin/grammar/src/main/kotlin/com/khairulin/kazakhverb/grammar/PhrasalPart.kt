package com.khairulin.kazakhverb.grammar

data class PhrasalPart(
    val partType: PhrasalPartType,
    val content: String,
    val aux: Boolean = false
) {
    fun copy(newContent: String): PhrasalPart {
        return PhrasalPart(partType, newContent, aux)
    }
}
