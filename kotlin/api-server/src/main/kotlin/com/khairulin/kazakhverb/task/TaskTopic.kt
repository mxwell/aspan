package com.khairulin.kazakhverb.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskTopic(val ruTitle: String) {
    CONJ_PRESENT_TRANSITIVE_EASY("Переходное время, лёгкий вариант"),
    CONJ_PRESENT_TRANSITIVE("Переходное время"),
    CONJ_PRESENT_CONTINUOUS_EASY("Настоящее время, лёгкий вариант"),
    CONJ_PRESENT_CONTINUOUS("Настоящее время"),
    CONJ_PAST_EASY("Прошедшее время, лёгкий вариант"),
    CONJ_PAST("Прошедшее время"),
    CONJ_REMOTE_PAST_EASY("Давнопрошедшее очевидное время, лёгкий вариант"),
    CONJ_REMOTE_PAST("Давнопрошедшее очевидное время"),
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