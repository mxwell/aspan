package com.khairulin.kazakhverb.task

import kotlinx.serialization.Serializable

@Serializable
enum class TaskTopic(val ruTitle: String) {
    CONJ_PRESENT_TRANSITIVE_EASY("Переходное время - 1"),
    CONJ_PRESENT_TRANSITIVE("Переходное время - 2"),
    CONJ_PRESENT_CONTINUOUS_EASY("Настоящее время - 1"),
    CONJ_PRESENT_CONTINUOUS("Настоящее время - 2"),
    CONJ_PAST_EASY("Прошедшее время - 1"),
    CONJ_PAST("Прошедшее время - 2"),
    CONJ_REMOTE_PAST_EASY("Давнопрошедшее очевидное время - 1"),
    CONJ_REMOTE_PAST("Давнопрошедшее очевидное время - 2"),
    CONJ_OPTATIVE_MOOD_EASY("Желательное наклонение - 1"),
    CONJ_OPTATIVE_MOOD("Желательное наклонение - 2"),
    CONJ_OPTATIVE_MOOD_PAST("Желательное наклонение - 3")
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
                CONJ_OPTATIVE_MOOD_EASY.name -> CONJ_OPTATIVE_MOOD_EASY
                CONJ_OPTATIVE_MOOD.name -> CONJ_OPTATIVE_MOOD
                CONJ_OPTATIVE_MOOD_PAST.name -> CONJ_OPTATIVE_MOOD_PAST
                else -> null
            }
        }
    }
}