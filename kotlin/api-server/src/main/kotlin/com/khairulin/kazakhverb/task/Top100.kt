package com.khairulin.kazakhverb.task

object Top100 {
    private val verbs: List<VerbEntry> by lazy {
        listOf(
            VerbEntry("болу", preceding = listOf("бұлай", "міндетті", "орталықта")),
            VerbEntry("келу", preceding = listOf("ертең", "қайта", "үйге", "жұмысқа")),
            VerbEntry("бару", preceding = listOf("дәрігерге", "алып", "қалаға атпен")),
            VerbEntry("тану", preceding = listOf("естен", "айтқаннан")),
            VerbEntry("тану", true, preceding = listOf("дауысынан", "дұрыс деп", "хат")),
        )
    }

    fun pickRandom() = verbs.random()
}