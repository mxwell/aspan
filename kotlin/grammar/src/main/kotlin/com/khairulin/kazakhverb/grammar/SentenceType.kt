package com.khairulin.kazakhverb.grammar

enum class SentenceType {
    Statement,
    Negative,
    Question,
    ;

    companion object {
        fun ofNegativeFlag(negativeFlag: Boolean): SentenceType {
            return if (negativeFlag) {
                Negative
            } else {
                Statement
            }
        }
    }
}