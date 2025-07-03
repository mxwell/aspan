package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.NounBuilder
import com.khairulin.kazakhverb.grammar.Septik

data class SupplementNoun(
    val noun: String,
    val translation: String,
    val septik: Septik?,
    val ownedBySubject: Boolean = false,  // the noun should be put into a possessive form determined by the subject
    val initialForm: String? = null,
) {

    companion object {
        fun notNoun(word: String, translation: String): SupplementNoun {
            return SupplementNoun(
                word,
                translation,
                septik = null,
                ownedBySubject = false,
            )
        }
    }

    fun builder() = NounBuilder.ofNoun(noun)

    fun form(subject: GrammarForm): String? {
        return if (ownedBySubject) {
            if (septik != null) {
                builder().possessiveSeptikForm(subject.person, subject.number, septik).raw
            } else {
                null
            }
        } else if (septik != null) {
            builder().septikForm(septik).raw
        } else {
            noun
        }
    }

    fun asPair(): Pair<String, String> {
        val from = if (initialForm != null) {
            initialForm
        } else {
            noun
        }
        return Pair(from, translation)
    }
}
