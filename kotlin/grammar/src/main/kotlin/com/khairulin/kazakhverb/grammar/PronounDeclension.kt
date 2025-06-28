package com.khairulin.kazakhverb.grammar

object PronounDeclension {
    private val kPronounInSeptik = mapOf<GrammarForm, List<String>>(
        GrammarForm.MEN to listOf("мен", "менің", "маған", "мені", "менде", "менен", "менімен"),
        GrammarForm.BIZ to listOf("біз", "біздің", "бізге", "бізді", "бізде", "бізден", "бізбен"),
        GrammarForm.SEN to listOf("сен", "сенің", "саған", "сені", "сенде", "сенен", "сенімен"),
        GrammarForm.SENDER to listOf("сендер", "сендердің", "сендерге", "сендерді", "сендерде", "сендерден", "сендермен"),
        GrammarForm.SIZ to listOf("Сіз", "Сіздің", "Сізге", "Сізді", "Сізде", "Сізден", "Сізбен"),
        GrammarForm.SIZDER to listOf("Сіздер", "Сіздердің", "Сіздерге", "Сіздерді", "Сіздерде", "Сіздерден", "Сіздермен"),
        GrammarForm.OL to listOf("ол", "оның", "оған", "оны", "онда", "одан", "онымен"),
        GrammarForm.OLAR to listOf("олар", "олардың", "оларға", "оларды", "оларда", "олардан", "олармен"),
    )

    fun getPronounForm(grammarForm: GrammarForm, septik: Septik): String {
        return kPronounInSeptik[grammarForm]!![septik.index]
    }

    private val kOzInAtau = mapOf<GrammarForm, String>(
        GrammarForm.MEN to "өзім",
        GrammarForm.BIZ to "өзіміз",
        GrammarForm.SEN to "өзің",
        GrammarForm.SENDER to "өздерің",
        GrammarForm.SIZ to "өзіңіз",
        GrammarForm.SIZDER to "өздеріңіз",
        GrammarForm.OL to "өзі",
        GrammarForm.OLAR to "өздері",
    )

    // Only Atau septik currently.
    fun getOzForm(grammarForm: GrammarForm): String {
        return kOzInAtau[grammarForm]!!
    }
}