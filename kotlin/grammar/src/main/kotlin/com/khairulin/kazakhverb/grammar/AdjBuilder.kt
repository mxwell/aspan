package com.khairulin.kazakhverb.grammar

class AdjBuilder(private val adjDictForm: String) {
    private val soft: Boolean
    private val softOffset: Int

    init {
        soft = Phonetics.wordIsSoft(adjDictForm)
        softOffset = Phonetics.softToOffset(soft)
    }

    private fun getLastBaseReplacement(last: Char): Char? {
        return when (last) {
            'к' -> 'г'
            'қ' -> 'ғ'
            'п' -> 'б'
            else -> null
        }
    }

    private fun getRakBase(last: Char): String {
        val repl = getLastBaseReplacement(last)
        if (repl == null) {
            return adjDictForm
        } else {
            return StrManip.replaceLast(adjDictForm, repl)
        }
    }

    private fun getRakAffix(last: Char): String {
        if (Phonetics.genuineVowel(last)) {
            return Rules.RAKREK[softOffset]
        } else {
            return Rules.YRAKIREK[softOffset]
        }
    }

    fun rakForm(): Phrasal {
        val last = adjDictForm.last()
        val base = getRakBase(last)
        val affix = getRakAffix(last)
        return PhrasalBuilder()
            .adjBase(base)
            .adjCompAffix(affix)
            .build()
    }
}