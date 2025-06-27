package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.VerbBuilder

data class VerbInfo(
    val verb: String,
    val forceExceptional: Boolean = false,
    val translation: String? = null,
) {
    fun builder() = VerbBuilder(verb, forceExceptional)
    fun asPair() = translation?.let { Pair(verb, it) }
    fun collectTranslations(dst: MutableList<List<String>>) {
        translation?.let {
            if (it.isNotEmpty()) {
                dst.add(listOf(verb, it))
            }
        }
    }
}
