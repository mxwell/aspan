package com.khairulin.kazakhverb.grammar

enum class GrammarForm(val person: GrammarPerson, val number: GrammarNumber, val pronoun: String, val poss: String, val dative: String, val ruShort: String) {
    MEN(GrammarPerson.First, GrammarNumber.Singular, "мен", "менің", "маған", "1л ед.ч."),
    BIZ(GrammarPerson.First, GrammarNumber.Plural, "біз", "біздің", "бізге", "1л мн.ч."),
    SEN(GrammarPerson.Second, GrammarNumber.Singular, "сен", "сенің", "саған", "2л ед.ч."),
    SENDER(GrammarPerson.Second, GrammarNumber.Plural, "сендер", "сендердің", "сендерге", "2л мн.ч."),
    SIZ(GrammarPerson.SecondPolite, GrammarNumber.Singular, "Сіз", "Сіздің", "Сізге", "уваж. 2л ед.ч."),
    SIZDER(GrammarPerson.SecondPolite, GrammarNumber.Plural, "Сіздер", "Сіздердің", "Сіздерге", "уваж. 2л мн.ч."),
    OL(GrammarPerson.Third, GrammarNumber.Singular, "ол", "оның", "оған", "3л ед.ч."),
    OLAR(GrammarPerson.Third, GrammarNumber.Plural, "олар", "олардың", "оларға", "3л мн.ч."),
    ;
}