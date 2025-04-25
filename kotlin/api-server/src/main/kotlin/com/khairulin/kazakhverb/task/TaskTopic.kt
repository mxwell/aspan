package com.khairulin.kazakhverb.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskTopic {
    CONJ_PRESENT_TRANSITIVE_EASY,
    CONJ_PRESENT_TRANSITIVE,
    CONJ_PRESENT_CONTINUOUS_EASY,
    CONJ_PRESENT_CONTINUOUS,
    CONJ_PAST_EASY,
    CONJ_PAST,
    CONJ_REMOTE_PAST_EASY,
    CONJ_REMOTE_PAST,
    ;

    companion object {
        fun of(value: String?): TaskTopic? {
            return when (value) {
                CONJ_PRESENT_TRANSITIVE_EASY.name -> CONJ_PRESENT_TRANSITIVE_EASY
                CONJ_PRESENT_TRANSITIVE.name -> CONJ_PRESENT_TRANSITIVE
                CONJ_PRESENT_CONTINUOUS_EASY.name -> CONJ_PRESENT_CONTINUOUS_EASY
                CONJ_PRESENT_CONTINUOUS.name -> CONJ_PRESENT_CONTINUOUS
                CONJ_PAST_EASY.name -> CONJ_PAST_EASY
                CONJ_PAST.name -> CONJ_PAST
                CONJ_REMOTE_PAST_EASY.name -> CONJ_REMOTE_PAST_EASY
                CONJ_REMOTE_PAST.name -> CONJ_REMOTE_PAST
                else -> null
            }
        }
    }
}