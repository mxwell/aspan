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
    CONJ_PAST_UNCERTAIN_EASY("Давнопрошедшее неочевидное время - 1"),
    CONJ_PAST_UNCERTAIN("Давнопрошедшее неочевидное время - 2"),
    CONJ_OPTATIVE_MOOD_EASY("Желательное наклонение - 1"),
    CONJ_OPTATIVE_MOOD("Желательное наклонение - 2"),
    CONJ_OPTATIVE_MOOD_PAST("Желательное наклонение - 3"),
    CONJ_CONDITIONAL_MOOD("Условное наклонение"),
    CONJ_CAN_CLAUSE_EASY("Конструкция с алу - 1"),
    CONJ_CAN_CLAUSE("Конструкция с алу - 2"),
    CONJ_CAN_CLAUSE_PAST("Конструкция с алу - 3"),
    CONJ_UNAU_CLAUSE("Конструкция с ұнау"),
    CONJ_UNATU_CLAUSE("Конструкция с ұнату"),
    CONJ_KORU_CLAUSE("Конструкция с жақсы, жек көру"),
    CONJ_TRY_CLAUSE_EASY("Конструкция с көру - 1"),
    CONJ_TRY_CLAUSE("Конструкция с көру - 2"),
    CONJ_JAZDAU_CLAUSE("Конструкция с жаздау"),
    CONJ_CONJUNCTIVE_AR("Сослагательное наклонение с едi - 1"),
    CONJ_CONJUNCTIVE_ATYN("Сослагательное наклонение с едi - 2"),
    CONJ_CONJUNCTIVE_USHY("Сослагательное наклонение с едi - 3"),
    DECL_TABYS_EASY("Винительный падеж - 1"),
    DECL_TABYS("Винительный падеж - 2"),
    DECL_ILIK_EASY("Родительный падеж - 1"),
    DECL_ILIK("Родительный падеж - 2"),
    DECL_BARYS_EASY("Дательный падеж - 1"),
    DECL_BARYS("Дательный падеж - 2"),
    DECL_JATYS_EASY("Местный падеж - 1"),
    DECL_JATYS("Местный падеж - 2"),
    DECL_SHYGYS_EASY("Исходный падеж - 1"),
    DECL_SHYGYS("Исходный падеж - 2"),
    DECL_KOMEKTES_EASY("Творительный падеж - 1"),
    DECL_KOMEKTES("Творительный падеж - 2"),
    ADJ_COMPARATIVE("Сравнительная степень"),
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
                CONJ_PAST_UNCERTAIN_EASY.name -> CONJ_PAST_UNCERTAIN_EASY
                CONJ_PAST_UNCERTAIN.name -> CONJ_PAST_UNCERTAIN
                CONJ_OPTATIVE_MOOD_EASY.name -> CONJ_OPTATIVE_MOOD_EASY
                CONJ_OPTATIVE_MOOD.name -> CONJ_OPTATIVE_MOOD
                CONJ_OPTATIVE_MOOD_PAST.name -> CONJ_OPTATIVE_MOOD_PAST
                CONJ_CONDITIONAL_MOOD.name -> CONJ_CONDITIONAL_MOOD
                CONJ_CAN_CLAUSE_EASY.name -> CONJ_CAN_CLAUSE_EASY
                CONJ_CAN_CLAUSE.name -> CONJ_CAN_CLAUSE
                CONJ_CAN_CLAUSE_PAST.name -> CONJ_CAN_CLAUSE_PAST
                CONJ_UNAU_CLAUSE.name -> CONJ_UNAU_CLAUSE
                CONJ_UNATU_CLAUSE.name -> CONJ_UNATU_CLAUSE
                CONJ_KORU_CLAUSE.name -> CONJ_KORU_CLAUSE
                CONJ_TRY_CLAUSE_EASY.name -> CONJ_TRY_CLAUSE_EASY
                CONJ_TRY_CLAUSE.name -> CONJ_TRY_CLAUSE
                CONJ_JAZDAU_CLAUSE.name -> CONJ_JAZDAU_CLAUSE
                CONJ_CONJUNCTIVE_AR.name -> CONJ_CONJUNCTIVE_AR
                CONJ_CONJUNCTIVE_ATYN.name -> CONJ_CONJUNCTIVE_ATYN
                CONJ_CONJUNCTIVE_USHY.name -> CONJ_CONJUNCTIVE_USHY
                DECL_TABYS_EASY.name -> DECL_TABYS_EASY
                DECL_TABYS.name -> DECL_TABYS
                DECL_ILIK_EASY.name -> DECL_ILIK_EASY
                DECL_ILIK.name -> DECL_ILIK
                DECL_BARYS_EASY.name -> DECL_BARYS_EASY
                DECL_BARYS.name -> DECL_BARYS
                DECL_JATYS_EASY.name -> DECL_JATYS_EASY
                DECL_JATYS.name -> DECL_JATYS
                DECL_SHYGYS_EASY.name -> DECL_SHYGYS_EASY
                DECL_SHYGYS.name -> DECL_SHYGYS
                DECL_KOMEKTES_EASY.name -> DECL_KOMEKTES_EASY
                DECL_KOMEKTES.name -> DECL_KOMEKTES
                ADJ_COMPARATIVE.name -> ADJ_COMPARATIVE
                else -> null
            }
        }
    }
}