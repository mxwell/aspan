package org.example

object PersAffix {
    fun getFirstPersAffix1LetterGroup(char: Char): PersAffix1LetterGroup {
        if (Rules.CONS_GROUP3.contains(char)) {
            return PersAffix1LetterGroup.PersAffix1GzGroup
        }
        if (Rules.CONS_GROUP4.contains(char) || Rules.CONS_GROUP5.contains(char)) {
            return PersAffix1LetterGroup.PersAffixUnvoicedGroup
        }
        if (Rules.CONS_GROUP6.contains(char)) {
            return PersAffix1LetterGroup.PersAffix1MnGroup
        }
        return PersAffix1LetterGroup.PersAffix1DefaultGroup
    }

    fun getFirstPersAffix1(number: GrammarNumber, char: Char, softOffset: Int): String {
        val group = getFirstPersAffix1LetterGroup(char)
        return Rules.FIRST_PERS_AFFIXES1[number]!![group]!![softOffset]
    }

    fun getPersAffix1(person: GrammarPerson, number: GrammarNumber, char: Char, softOffset: Int): String {
        if (person == GrammarPerson.First) {
            return getFirstPersAffix1(number, char, softOffset)
        }
        if (person == GrammarPerson.Second) {
            return Rules.SECOND_PERS_AFFIXES1[number]!![softOffset]
        }
        if (person == GrammarPerson.SecondPolite) {
            return Rules.SECOND_POLITE_PERS_AFFIXES1[number]!![softOffset]
        }
        return ""
    }
}