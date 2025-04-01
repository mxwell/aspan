package org.example

import kotlinx.serialization.Serializable

@Serializable
data class PlusFormSet(
    val Statement: List<String>,
    val Negative: List<String>,
    val Question: List<String>
) {
    fun getForms(sentenceType: SentenceType): List<Phrasal> {
        val plusForms = when (sentenceType) {
            SentenceType.Statement -> Statement
            SentenceType.Negative -> Negative
            SentenceType.Question -> Question
        }
        return plusForms.map {
            val builder = PhrasalBuilder()
            for (word in it.split("+")) {
                builder.unclassified(word)
            }
            builder.build()
        }
    }
}
