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
    CONJ_OPTATIVE_MOOD_PAST("Желательное наклонение - 3"),
    CONJ_CAN_CLAUSE_EASY("Конструкция с алу - 1"),
    CONJ_CAN_CLAUSE("Конструкция с алу - 2"),
    CONJ_CAN_CLAUSE_PAST("Конструкция с алу - 3"),
    CONJ_UNAU_CLAUSE("Конструкция с ұнау"),
    CONJ_UNATU_CLAUSE("Конструкция с ұнату"),
    CONJ_KORU_CLAUSE("Конструкция с жақсы, жек көру"),
    DECL_TABYS_EASY("Винительный падеж - 1"),
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
                CONJ_CAN_CLAUSE_EASY.name -> CONJ_CAN_CLAUSE_EASY
                CONJ_CAN_CLAUSE.name -> CONJ_CAN_CLAUSE
                CONJ_CAN_CLAUSE_PAST.name -> CONJ_CAN_CLAUSE_PAST
                CONJ_UNAU_CLAUSE.name -> CONJ_UNAU_CLAUSE
                CONJ_UNATU_CLAUSE.name -> CONJ_UNATU_CLAUSE
                CONJ_KORU_CLAUSE.name -> CONJ_KORU_CLAUSE
                DECL_TABYS_EASY.name -> DECL_TABYS_EASY
                else -> null
            }
        }
    }
}