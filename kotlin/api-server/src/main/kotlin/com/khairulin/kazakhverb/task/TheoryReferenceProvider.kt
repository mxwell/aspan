package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.response.TheoryReference
import com.khairulin.kazakhverb.response.TheoryReferenceType

object TheoryReferenceProvider {
    private fun makeKazTiliArticle(name: String, path: String): TheoryReference {
        assert(path.startsWith("/"))

        return TheoryReference(
            TheoryReferenceType.URL_ARTICLE,
            "kaz-tili.kz → ${name}",
            "Татьяна Валяева",
            "https://www.kaz-tili.kz${path}"
        )
    }

    private fun makeYtKazakhtili(name: String, videoId: String) =
        TheoryReference(TheoryReferenceType.URL_VIDEO, "Казахский язык для всех! → ${name}", "Назира Жылқыбаева", "https://www.youtube.com/watch?v=${videoId}")
    private fun makeBookBekturovs(name: String, page: Int) =
        TheoryReference(TheoryReferenceType.BOOK, "Казахский язык для всех → ${name}", "А.Ш. Бектурова, Ш.К. Бектуров", "2004 год, страница ${page}")

    private val tensePresentTransitive = listOf(
        makeKazTiliArticle("Переходное время глагола", "/glag2.htm"),
        makeYtKazakhtili("Настоящее переходное время и переходное будущее время казахского языка", "SuR_nl1ba0E"),
        makeBookBekturovs("Грамматика: Ауыспалы келер шақ", 203),
    )
    private val tensePresentContinuous = listOf(
        makeKazTiliArticle("Настоящее время глагола", "/glag1.htm"),
        makeYtKazakhtili("Настоящее время казахского языка (простая форма). Нақ осы шақ", "unjngktuvc8"),
        makeYtKazakhtili("Настоящее время казахского языка (сложная форма)", "jZPl0FJck04"),
        makeYtKazakhtili("Настоящее время казахского языка (сложная форма) 2 часть", "ghjRR1CnVDg"),
        makeBookBekturovs("Настоящее время глагола. Простая форма настоящего времени", 130),
        makeBookBekturovs("Грамматика: сложная форма очевидного настоящего времени", 141),
    )
    private val tensePast = listOf(
        makeKazTiliArticle("Прошедшее время глагола", "/glag3.htm"),
        makeYtKazakhtili("Жедел өткен шақ - Очевидное прошедшее время казахского языка", "FFXCc2uJ-b8"),
        makeBookBekturovs("Грамматика: жедел өткен шақ", 335),
    )
    private val tenseRemotePast = listOf(
        makeKazTiliArticle("Давнопрошедшее очевидное время глагола", "/glag9.htm"),
        makeYtKazakhtili("Достоверное прошедшее время казахского языка", "QLjwHyGcgL0"),
        makeBookBekturovs("Грамматика: Есімше. Бұрынғы өткен шақ", 377),
    )
    private val tensePastUncertain = listOf(
        makeKazTiliArticle("Давнопрошедшее неочевидное время глагола", "/glag10.htm"),
        makeYtKazakhtili("Давнопрошедшее (неочевидное прошедшее) время казахского языка", "qW_qbkA_x6M"),
        makeBookBekturovs("Бұрынғы өткен шақ (ып/іп/п)", 393),
    )
    private val moodOptative = listOf(
        makeKazTiliArticle("Желательное наклонение глагола", "/glag5.htm"),
        makeYtKazakhtili("Как скажем на казахском \"Я ХОЧУ\"", "SxpsZqKp-m4"),
        makeBookBekturovs("Грамматика: қалау рай", 273),
    )
    private val moodConditional = listOf(
        makeKazTiliArticle("Условное наклонение глагола", "/glag4.htm"),
        makeYtKazakhtili("Как скажем на казахском \"Если...\"", "Y4eMzYdxEMQ"),
        // TODO find book page
    )
    private val clauseCan = listOf(
        makeKazTiliArticle("алу – мочь,  бiлу – уметь", "/modal1.htm"),
        makeYtKazakhtili("Как скажем на казахском \"Я МОГУ\"", "OGn0bbqhlw4"),
    )
    private val clauseUnau = listOf(
        makeKazTiliArticle("ұнау, ұнату – нравиться, жақсы көру – любить, жек көру – ненавидеть", "/modal6.htm"),
    )
    private val clauseUnatu = listOf(
        makeKazTiliArticle("ұнау, ұнату – нравиться, жақсы көру – любить, жек көру – ненавидеть", "/modal6.htm"),
    )
    private val clauseKoru = listOf(
        makeKazTiliArticle("ұнау, ұнату – нравиться, жақсы көру – любить, жек көру – ненавидеть", "/modal6.htm"),
    )
    private val clauseTry = listOf(
        makeKazTiliArticle("Вспомогательный глагол 'көру'", "/glv03.htm"),
        makeYtKazakhtili("Как скажем на казахском \"попробовать, попытаться\"", "NED0itSbEQ8"),
    )
    private val clauseJazdau = listOf(
        makeKazTiliArticle("Вспомогательный глагол 'жаздау'", "/glv08.htm"),
    )
    private val conjunctiveAr = listOf(
        makeKazTiliArticle("Вспомогательный глагол 'едi'", "/glv02.htm#metka3"),
    )
    private val conjunctiveAtyn = listOf(
        makeKazTiliArticle("Вспомогательный глагол 'едi'", "/glv02.htm#metka3"),
    )
    private val emptyRefs = emptyList<TheoryReference>()
    private val septikTabys = listOf(
        makeKazTiliArticle("Винительный падеж", "/su_rod2.htm"),
        makeYtKazakhtili("Винительный падеж казахского языка", "maBkCk8oGC0"),
        makeBookBekturovs("Винительный падеж имен существительных", 48),
    )
    private val septikIlik = listOf(
        makeKazTiliArticle("Родительный падеж", "/su_rod1.htm"),
        makeYtKazakhtili("Родительный падеж казахского языка", "maIcnpxAk1I"),
        makeBookBekturovs("Грамматика: родительный падеж имен существительных", 107),
    )
    private val septikBarys = listOf(
        makeKazTiliArticle("Дательно-направительный падеж", "/su_mesto3.htm"),
        makeYtKazakhtili("Дательно направительный падеж казахского языка", "uJrtGEsNH7g"),
        makeBookBekturovs("Дательно-направительный падеж имен существительных", 86),
    )
    private val septikJatys = listOf(
        makeKazTiliArticle("Местный падеж", "/su_mesto2.htm"),
        makeYtKazakhtili("Местный падеж казахского языка", "gxTHC1eSSWk"),
        makeBookBekturovs("Местный падеж имени существительного", 32),
    )
    private val septikShygys = listOf(
        makeKazTiliArticle("Исходный падеж", "/su_mesto1.htm"),
        makeYtKazakhtili("Исходный падеж казахского языка", "DfHehYM4hXs"),
        makeBookBekturovs("Шығыс септік", 165),
    )
    private val septikKomektes = listOf(
        makeKazTiliArticle("Творительный падеж", "/su_tvorit.htm"),
        makeYtKazakhtili("Көмектес септік, творительный падеж казахского языка", "aFuxxgPLXP0"),
        makeBookBekturovs("Көмектес септік", 241),
    )
    private val adjComparativeRak = listOf(
        makeKazTiliArticle("Степени сравнения прилагательных", "/prilag2.htm"),
        makeYtKazakhtili("Степени сравнения прилагательных в казахском языке", "Dv9iAcuwZM4"),
        makeBookBekturovs("Грамматика: сын есiмнің шырайлары", 230),
    )
    private val adjComparativeDau = listOf(
        makeKazTiliArticle("Степени сравнения прилагательных", "/prilag2.htm"),
        makeYtKazakhtili("Степени сравнения прилагательных в казахском языке, 2 часть", "NVs8wnJVyOQ"),
        makeBookBekturovs("Грамматика: сын есiмнің шырайлары", 230),
    )

    fun provide(topic: TaskTopic): List<TheoryReference> {
        return when(topic) {
            TaskTopic.CONJ_PRESENT_TRANSITIVE_EASY, TaskTopic.CONJ_PRESENT_TRANSITIVE -> tensePresentTransitive
            TaskTopic.CONJ_PRESENT_CONTINUOUS_EASY, TaskTopic.CONJ_PRESENT_CONTINUOUS -> tensePresentContinuous
            TaskTopic.CONJ_PAST_EASY, TaskTopic.CONJ_PAST -> tensePast
            TaskTopic.CONJ_REMOTE_PAST_EASY, TaskTopic.CONJ_REMOTE_PAST -> tenseRemotePast
            TaskTopic.CONJ_PAST_UNCERTAIN_EASY, TaskTopic.CONJ_PAST_UNCERTAIN -> tensePastUncertain
            TaskTopic.CONJ_OPTATIVE_MOOD_EASY, TaskTopic.CONJ_OPTATIVE_MOOD, TaskTopic.CONJ_OPTATIVE_MOOD_PAST -> moodOptative
            TaskTopic.CONJ_CONDITIONAL_MOOD -> moodConditional
            TaskTopic.CONJ_CAN_CLAUSE_EASY, TaskTopic.CONJ_CAN_CLAUSE, TaskTopic.CONJ_CAN_CLAUSE_PAST -> clauseCan
            TaskTopic.CONJ_UNAU_CLAUSE -> clauseUnau
            TaskTopic.CONJ_UNATU_CLAUSE -> clauseUnatu
            TaskTopic.CONJ_KORU_CLAUSE -> clauseKoru
            TaskTopic.CONJ_TRY_CLAUSE_EASY, TaskTopic.CONJ_TRY_CLAUSE -> clauseTry
            TaskTopic.CONJ_JAZDAU_CLAUSE -> clauseJazdau
            TaskTopic.CONJ_CONJUNCTIVE_AR -> conjunctiveAr
            TaskTopic.CONJ_CONJUNCTIVE_ATYN -> conjunctiveAtyn
            TaskTopic.CONJ_CONJUNCTIVE_USHY -> emptyRefs
            TaskTopic.DECL_TABYS_EASY, TaskTopic.DECL_TABYS -> septikTabys
            TaskTopic.DECL_ILIK_EASY, TaskTopic.DECL_ILIK -> septikIlik
            TaskTopic.DECL_BARYS_EASY, TaskTopic.DECL_BARYS -> septikBarys
            TaskTopic.DECL_JATYS_EASY, TaskTopic.DECL_JATYS -> septikJatys
            TaskTopic.DECL_SHYGYS_EASY, TaskTopic.DECL_SHYGYS -> septikShygys
            TaskTopic.DECL_KOMEKTES_EASY, TaskTopic.DECL_KOMEKTES -> septikKomektes
            TaskTopic.ADJ_COMPARATIVE -> adjComparativeRak
            TaskTopic.ADJ_COMPARATIVE_DAU -> adjComparativeDau
        }
    }
}