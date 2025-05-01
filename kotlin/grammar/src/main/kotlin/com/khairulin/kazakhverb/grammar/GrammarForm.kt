package com.khairulin.kazakhverb.grammar

enum class GrammarForm(val person: GrammarPerson, val number: GrammarNumber, val pronoun: String, val poss: String, val dative: String) {
    MEN(GrammarPerson.First, GrammarNumber.Singular, "мен", "менің", "маған"),
    BIZ(GrammarPerson.First, GrammarNumber.Plural, "біз", "біздің", "бізге"),
    SEN(GrammarPerson.Second, GrammarNumber.Singular, "сен", "сенің", "саған"),
    SENDER(GrammarPerson.Second, GrammarNumber.Plural, "сендер", "сендердің", "сендерге"),
    SIZ(GrammarPerson.SecondPolite, GrammarNumber.Singular, "Сіз", "Сіздің", "Сізге"),
    SIZDER(GrammarPerson.SecondPolite, GrammarNumber.Plural, "Сіздер", "Сіздердің", "Сіздерге"),
    OL(GrammarPerson.Third, GrammarNumber.Singular, "ол", "оның", "оған"),
    OLAR(GrammarPerson.Third, GrammarNumber.Plural, "олар", "олардың", "оларға"),
    ;
}