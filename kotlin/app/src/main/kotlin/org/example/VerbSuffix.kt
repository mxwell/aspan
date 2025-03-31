package org.example

object VerbSuffix {
    fun gangenCompatible(char: Char): Boolean {
        return Rules.VOWELS.contains(char) || Rules.CONS_GROUP1.contains(char) || Rules.CONS_GROUP2.contains(char)
    }

    fun getGangenKanken(char: Char, softOffset: Int): String {
        if (gangenCompatible(char)) {
            return Rules.GANGEN[softOffset]
        }
        return Rules.KANKEN[softOffset]
    }

    fun kykiCompatible(char: Char): Boolean {
        return Rules.CONS_GROUP4.contains(char) || Rules.CONS_GROUP5.contains(char)
    }

    fun getGygiKyki(char: Char, softOffset: Int): String {
        if (kykiCompatible(char)) {
            return Rules.KYKI[softOffset]
        }
        return Rules.GYGI[softOffset]
    }

    fun getDydiTyti(char: Char, softOffset: Int): String {
        if (kykiCompatible(char)) {
            return Rules.TYTI[softOffset]
        }
        return Rules.DYDI[softOffset]
    }

    fun getYpip(char: Char, softOffset: Int): String {
        if (Phonetics.genuineVowel(char)) {
            return "п"
        }
        return Rules.YPIP[softOffset]
    }

    fun getImperativeVowel(person: GrammarPerson, number: GrammarNumber, char: Char, softOffset: Int): String {
        if (person == GrammarPerson.First) {
            if (Phonetics.genuineVowel(char)) {
                return ""
            }
            return Rules.AE[softOffset]
        }
        if (person == GrammarPerson.Second) {
            if (number == GrammarNumber.Singular || Phonetics.genuineVowel(char)) {
                return ""
            }
            return Rules.YI[softOffset]
        }
        if (person == GrammarPerson.SecondPolite) {
            if (Phonetics.genuineVowel(char)) {
                return ""
            }
            return Rules.YI[softOffset]
        }
        return ""
    }

    fun getImperativeAffix(person: GrammarPerson, number: GrammarNumber, char: Char, softOffset: Int): String {
        val vowel = getImperativeVowel(person, number, char, softOffset)
        val affix = Rules.IMPERATIVE_AFFIXES[person]!![number]!![softOffset]
        return "${vowel}${affix}"
    }
}