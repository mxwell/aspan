package com.khairulin.kazakhverb.vm

enum class TenseId(val title: String, val description: String) {
    presentTransitive("Present Indefinite tense", "Habitual actions or certain future. Time determined by context"),
    presentContinuous("Present Continuous tense", "Action occurring at the current moment"),
    past("Simple Past tense", "Past action without specifying exact time"),
    remotePast("Remote Past tense", "Action in distant past, witnessed by the speaker"),
    conditionalMood("Conditional mood", "Desired or possible action under certain conditions"),
    imperativeMood("Imperative mood", "Urging to action in the form of command, request, advice"),
    optativeMood("Optative mood", "Desire or intention of the speaker or others"),
    ;
}