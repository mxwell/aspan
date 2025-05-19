package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.SentenceType

enum class SentenceTypePattern {
    S10,
    S6_N2_Q2,
    S7_Q3,
    ;

    fun getSentenceTypeByTaskId(taskId: Int): SentenceType {
        if (this == S6_N2_Q2) {
            if (taskId in 7..8) {
                return SentenceType.Negative
            } else if (taskId >= 9) {
                return SentenceType.Question
            }
        } else if (this == S7_Q3) {
            if (taskId >= 8) {
                return SentenceType.Question
            }
        }
        return SentenceType.Statement
    }
}