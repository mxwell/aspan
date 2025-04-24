package com.khairulin.kazakhverb.grammar

enum class GrammarForm(val person: GrammarPerson, val number: GrammarNumber, val pronoun: String, val poss: String) {
    MEN(GrammarPerson.First, GrammarNumber.Singular, "мен", "менің"),
    BIZ(GrammarPerson.First, GrammarNumber.Plural, "біз", "біздің"),
    SEN(GrammarPerson.Second, GrammarNumber.Singular, "сен", "сенің"),
    SENDER(GrammarPerson.Second, GrammarNumber.Plural, "сендер", "сендердің"),
    SIZ(GrammarPerson.SecondPolite, GrammarNumber.Singular, "Сіз", "Сіздің"),
    SIZDER(GrammarPerson.SecondPolite, GrammarNumber.Plural, "Сіздер", "Сіздердің"),
    OL(GrammarPerson.Third, GrammarNumber.Singular, "ол", "оның"),
    OLAR(GrammarPerson.Third, GrammarNumber.Plural, "олар", "олардың"),
    ;
}