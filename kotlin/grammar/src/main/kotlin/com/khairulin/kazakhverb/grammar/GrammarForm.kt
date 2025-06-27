package com.khairulin.kazakhverb.grammar

enum class GrammarForm(val person: GrammarPerson, val number: GrammarNumber, val pronoun: String, val poss: String, val dative: String, val jatys: String, val ruShort: String) {
    MEN(GrammarPerson.First, GrammarNumber.Singular, "мен", "менің", "маған", "менде", "1л ед.ч."),
    BIZ(GrammarPerson.First, GrammarNumber.Plural, "біз", "біздің", "бізге", "бізде", "1л мн.ч."),
    SEN(GrammarPerson.Second, GrammarNumber.Singular, "сен", "сенің", "саған", "сенде", "2л ед.ч."),
    SENDER(GrammarPerson.Second, GrammarNumber.Plural, "сендер", "сендердің", "сендерге", "сендерде", "2л мн.ч."),
    SIZ(GrammarPerson.SecondPolite, GrammarNumber.Singular, "Сіз", "Сіздің", "Сізге", "Сізде", "уваж. 2л ед.ч."),
    SIZDER(GrammarPerson.SecondPolite, GrammarNumber.Plural, "Сіздер", "Сіздердің", "Сіздерге", "Сіздерде", "уваж. 2л мн.ч."),
    OL(GrammarPerson.Third, GrammarNumber.Singular, "ол", "оның", "оған", "онда", "3л ед.ч."),
    OLAR(GrammarPerson.Third, GrammarNumber.Plural, "олар", "олардың", "оларға", "оларда", "3л мн.ч."),
    ;

    fun getPronounByTense(tense: VerbTense): String {
        if (tense == VerbTense.MoodOptative) {
            return poss
        }
        return pronoun
    }

    companion object {
        val kMainForms = listOf(
            GrammarForm.MEN,
            GrammarForm.BIZ,
            GrammarForm.SEN,
            GrammarForm.SIZ,
            GrammarForm.OL,
            GrammarForm.OLAR,
        )

        fun getMainRandom() = kMainForms.random()
    }
}