package org.example

object Question {
    fun getQuestionParticle(char: Char, softOffset: Int): String {
        return AffixHelpers.chooseMBP(char, softOffset, Rules.MAME, Rules.BABE, Rules.PAPE)
    }
}