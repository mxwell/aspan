package com.khairulin.kazakhverb.grammar

object AffixHelpers {
    fun mameCompatible(char: Char): Boolean {
        return Rules.VOWELS.contains(char) || Rules.CONS_GROUP1.contains(char)
    }

    fun babeCompatible(char: Char): Boolean {
        return Rules.CONS_GROUP2.contains(char)
    }

    fun chooseMBP(char: Char, softOffset: Int, mAffixes: List<String>, bAffixes: List<String>, pAffixes: List<String>): String {
        if (mameCompatible(char = char)) {
            return mAffixes[softOffset]
        }
        if (babeCompatible(char = char)) {
            return bAffixes[softOffset]
        }
        return pAffixes[softOffset]
    }

    fun chooseLDT(char: Char, softOffset: Int, lAffixes: List<String>, dAffixes: List<String>, tAffixes: List<String>): String {
        if (Phonetics.isVowel(char) || Rules.CONS_GROUP7.contains(char)) {
            return lAffixes[softOffset]
        }
        if (Rules.CONS_GROUP8.contains(char)) {
            return dAffixes[softOffset]
        }
        return tAffixes[softOffset]
    }
}