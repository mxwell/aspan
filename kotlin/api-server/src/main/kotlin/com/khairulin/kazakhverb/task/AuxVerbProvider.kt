package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.VerbBuilder

object AuxVerbProvider {
    val jatuBuilder: VerbBuilder by lazy {
        VerbBuilder("жату")
    }
    val otyruBuilder: VerbBuilder by lazy {
        VerbBuilder("отыру")
    }
    val turuBuilder: VerbBuilder by lazy {
        VerbBuilder("тұру")
    }
}