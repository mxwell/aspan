package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import org.slf4j.LoggerFactory

/**
 * Usage:
 *   $ ./gradlew :test:run --args="Conjugation"
 */
class ConjugationTest() {
    private val LOG = LoggerFactory.getLogger(this.javaClass)

    private fun checkFormString(expected: String, phrasal: Phrasal, ctx: String? = null) {
        val actual = phrasal.raw
        if (expected != actual) {
            val context = ctx?.let { " -- ${it}" } ?: ""
            LOG.warn("unexpected form: ${actual} instead of ${expected}${context}")
        }
    }

    private fun testJazdau() {
        val kaluBuilder = VerbBuilder("қалу")
        val saluBuilder = VerbBuilder("салу")
        checkFormString(
            "ұмытып қала жаздадым",
            VerbBuilder("ұмыту").jazdauClause(GrammarPerson.First, GrammarNumber.Singular, kaluBuilder)
        )
        checkFormString(
            "айтып сала жаздады",
            VerbBuilder("айту").jazdauClause(GrammarPerson.Third, GrammarNumber.Singular, saluBuilder)
        )
    }

    private fun testPastUncertain() {
        val relations = mapOf<String, List<String>>(
            "жазу" to listOf(
                "жазыппын", "жазыппыз", "жазыпсың", "жазыпсыңдар", "жазыпсыз", "жазыпсыздар", "жазыпты", "жазыпты",
                "жазбаппын", "жазбаппыз", "жазбапсың", "жазбапсыңдар", "жазбапсыз", "жазбапсыздар", "жазбапты", "жазбапты"
            ),
            "көру" to listOf(
                "көріппін", "көріппіз", "көріпсің", "көріпсіңдер", "көріпсіз", "көріпсіздер", "көріпті", "көріпті",
                "көрмеппін", "көрмеппіз", "көрмепсің", "көрмепсіңдер", "көрмепсіз", "көрмепсіздер", "көрмепті", "көрмепті"
            ),
            "ойнау" to listOf(
                "ойнаппын", "ойнаппыз", "ойнапсың", "ойнапсыңдар", "ойнапсыз", "ойнапсыздар", "ойнапты", "ойнапты",
                "ойнамаппын", "ойнамаппыз", "ойнамапсың", "ойнамапсыңдар", "ойнамапсыз", "ойнамапсыздар", "ойнамапты", "ойнамапты"
            ),
            "оқу" to listOf(
                "оқыппын", "оқыппыз", "оқыпсың", "оқыпсыңдар", "оқыпсыз", "оқыпсыздар", "оқыпты", "оқыпты",
                "оқымаппын", "оқымаппыз", "оқымапсың", "оқымапсыңдар", "оқымапсыз", "оқымапсыздар", "оқымапты", "оқымапты"
            ),
            "қою" to listOf(
                "қойыппын", "қойыппыз", "қойыпсың", "қойыпсыңдар", "қойыпсыз", "қойыпсыздар", "қойыпты", "қойыпты",
                "қоймаппын", "қоймаппыз", "қоймапсың", "қоймапсыңдар", "қоймапсыз", "қоймапсыздар", "қоймапты", "қоймапты"
            ),
            "қорқу" to listOf(
                "қорқыппын", "қорқыппыз", "қорқыпсың", "қорқыпсыңдар", "қорқыпсыз", "қорқыпсыздар", "қорқыпты", "қорқыпты",
                "қорықпаппын", "қорықпаппыз", "қорықпапсың", "қорықпапсыңдар", "қорықпапсыз", "қорықпапсыздар", "қорықпапты", "қорықпапты"
            ),
            "ірку" to listOf(
                "іркіппін", "іркіппіз", "іркіпсің", "іркіпсіңдер", "іркіпсіз", "іркіпсіздер", "іркіпті", "іркіпті",
                "ірікпеппін", "ірікпеппіз", "ірікпепсің", "ірікпепсіңдер", "ірікпепсіз", "ірікпепсіздер", "ірікпепті", "ірікпепті"
            ),
        )

        for ((verb, forms) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                for (person in GrammarPerson.entries) {
                    for (number in GrammarNumber.entries) {
                        val phrasal =
                            builder.pastUncertainTense(person, number, sentenceType)
                        val expected = forms[position]
                        position += 1
                        checkFormString(
                            expected,
                            phrasal,
                            "person ${person}, number ${number}, sentence ${sentenceType}"
                        )
                    }
                }
            }
        }
    }

    private fun testIntentionFuture() {
        val relations = mapOf<String, List<String>>(
            "ұғу" to listOf(
                "ұқпақпын", "ұқпақпыз", "ұқпақсың", "ұқпақсыңдар", "ұқпақсыз", "ұқпақсыздар", "ұқпақ", "ұқпақ",
                "ұқпақ емеспін", "ұқпақ емеспіз", "ұқпақ емессің", "ұқпақ емессіңдер", "ұқпақ емессіз", "ұқпақ емессіздер", "ұқпақ емес", "ұқпақ емес",
            ),
            "көру" to listOf(
                "көрмекпін", "көрмекпіз", "көрмексің", "көрмексіңдер", "көрмексіз", "көрмексіздер", "көрмек", "көрмек",
                "көрмек емеспін", "көрмек емеспіз", "көрмек емессің", "көрмек емессіңдер", "көрмек емессіз", "көрмек емессіздер", "көрмек емес", "көрмек емес",
            ),
        )

        for ((verb, forms) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                for (person in GrammarPerson.entries) {
                    for (number in GrammarNumber.entries) {
                        val phrasal = builder.intentionFutureForm(person, number, sentenceType)
                        val expected = forms[position]
                        position += 1
                        checkFormString(
                            expected,
                            phrasal,
                            "person ${person}, number ${number}, sentence ${sentenceType}"
                        )
                    }
                }
            }
        }
    }

    private fun testPossibleFuture() {
        val relations = mapOf<String, List<String>>(
            "қорқу" to listOf(
                "қорқармын", "қорқармыз", "қорқарсың", "қорқарсыңдар", "қорқарсыз", "қорқарсыздар", "қорқар", "қорқар",
                "қорықпаспын", "қорықпаспыз", "қорықпассың", "қорықпассыңдар", "қорықпассыз", "қорықпассыздар", "қорықпас", "қорықпас"
            ),
            "көру" to listOf(
                "көрермін", "көрерміз", "көрерсің", "көрерсіңдер", "көрерсіз", "көрерсіздер", "көрер", "көрер",
                "көрмеспін", "көрмеспіз", "көрмессің", "көрмессіңдер", "көрмессіз", "көрмессіздер", "көрмес", "көрмес",
            ),
        )

        for ((verb, forms) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                for (person in GrammarPerson.entries) {
                    for (number in GrammarNumber.entries) {
                        val phrasal = builder.possibleFutureForm(person, number, sentenceType)
                        val expected = forms[position]
                        position += 1
                        checkFormString(
                            expected,
                            phrasal,
                            "person ${person}, number ${number}, sentence ${sentenceType}"
                        )
                    }
                }
            }
        }
    }

    fun testPastTransitive() {
        val relations = mapOf<String, List<String>>(
            "жазу" to listOf(
                "жазатынмын", "жазатынбыз", "жазатынсың", "жазатынсыңдар", "жазатынсыз", "жазатынсыздар", "жазатын", "жазатын",
                "жазбайтынмын", "жазбайтынбыз", "жазбайтынсың", "жазбайтынсыңдар", "жазбайтынсыз", "жазбайтынсыздар", "жазбайтын", "жазбайтын",
            ),
            "көру" to listOf(
                "көретінмін", "көретінбіз", "көретінсің", "көретінсіңдер", "көретінсіз", "көретінсіздер", "көретін", "көретін",
                "көрмейтінмін", "көрмейтінбіз", "көрмейтінсің", "көрмейтінсіңдер", "көрмейтінсіз", "көрмейтінсіздер", "көрмейтін", "көрмейтін",
            ),
            "төлеу" to listOf(
                "төлейтінмін", "төлейтінбіз", "төлейтінсің", "төлейтінсіңдер", "төлейтінсіз", "төлейтінсіздер", "төлейтін", "төлейтін",
                "төлемейтінмін", "төлемейтінбіз", "төлемейтінсің", "төлемейтінсіңдер", "төлемейтінсіз", "төлемейтінсіздер", "төлемейтін", "төлемейтін",
            ),
            "оқу" to listOf(
                "оқитынмын", "оқитынбыз", "оқитынсың", "оқитынсыңдар", "оқитынсыз", "оқитынсыздар", "оқитын", "оқитын",
                "оқымайтынмын", "оқымайтынбыз", "оқымайтынсың", "оқымайтынсыңдар", "оқымайтынсыз", "оқымайтынсыздар", "оқымайтын", "оқымайтын",
            ),
            "қою" to listOf(
                "қоятынмын", "қоятынбыз", "қоятынсың", "қоятынсыңдар", "қоятынсыз", "қоятынсыздар", "қоятын", "қоятын",
                "қоймайтынмын", "қоймайтынбыз", "қоймайтынсың", "қоймайтынсыңдар", "қоймайтынсыз", "қоймайтынсыздар", "қоймайтын", "қоймайтын",
            )
        )

        for ((verb, forms) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                for (person in GrammarPerson.entries) {
                    for (number in GrammarNumber.entries) {
                        val phrasal = builder.pastTransitiveTense(person, number, sentenceType)
                        val expected = forms[position]
                        position += 1
                        checkFormString(
                            expected,
                            phrasal,
                            "person ${person}, number ${number}, sentence ${sentenceType}"
                        )
                    }
                }
            }
        }
    }

    private fun testPresentParticiple() {
        val relations = mapOf<String, List<String>>(
            "түсіну" to listOf("түсінетін", "түсінбейтін"),
            "күлу" to listOf("күлетін", "күлмейтін"),
            "қарау" to listOf("қарайтын", "қарамайтын"),
            "жеу" to listOf("жейтін", "жемейтін"),
        )

        for ((verb, expectations) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                val phrasal = builder.presentParticiple(sentenceType)
                val expected = expectations[position]
                position += 1
                checkFormString(
                    expected,
                    phrasal,
                    "sentence ${sentenceType}"
                )
            }
        }
    }

    private fun testPastParticiple() {
        val relations = mapOf<String, List<String>>(
            "түсіну" to listOf("түсінген", "түсінбеген"),
            "күлу" to listOf("күлген", "күлмеген"),
            "қарау" to listOf("қараған", "қарамаған"),
            "жеу" to listOf("жеген", "жемеген"),
        )

        for ((verb, expectations) in relations) {
            val builder = VerbBuilder(verb)
            var position = 0
            for (sentenceType in listOf(SentenceType.Statement, SentenceType.Negative)) {
                val phrasal = builder.pastParticiple(sentenceType)
                val expected = expectations[position]
                position += 1
                checkFormString(
                    expected,
                    phrasal,
                    "sentence ${sentenceType}"
                )
            }
        }
    }

    private fun testUshyUshiForm() {
        checkFormString(
            "ұнатушы",
            VerbBuilder("ұнату").ushyUshiForm(SentenceType.Statement),
            "verb ұнату",
        )
        checkFormString(
            "түсіндіруші",
            VerbBuilder("түсіндіру").ushyUshiForm(SentenceType.Statement),
            "verb түсіндіру",
        )
        checkFormString(
            "сөйлесуші",
            VerbBuilder("сөйлесу").ushyUshiForm(SentenceType.Statement),
            "verb сөйлесу",
        )
        checkFormString(
            "ұмытушы",
            VerbBuilder("ұмыту").ushyUshiForm(SentenceType.Statement),
            "verb ұмыту",
        )
        checkFormString(
            "шықпаушы",
            VerbBuilder("шығу").ushyUshiForm(SentenceType.Negative),
            "verb шығу",
        )
    }

    private fun testPresentContinuous() {
        val auxBuilder = VerbBuilder("жату")
        checkFormString(
            "бара жатырмын",
            VerbBuilder("бару").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "preset cont. for бару",
        )
        checkFormString(
            "алып бара жатырмын",
            VerbBuilder("алып бару").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "preset cont. for алып бару",
        )
        checkFormString(
            "келе жатырмын",
            VerbBuilder("келу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "preset cont. for келу",
        )
        checkFormString(
            "барып келе жатырмын",
            VerbBuilder("барып келу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "preset cont. for барып келу",
        )
        checkFormString(
            "жазып жатырмын",
            VerbBuilder("жазу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "preset cont. for жазу",
        )
    }

    fun test() {
        testJazdau()
        testPastUncertain()
        testPossibleFuture()
        testIntentionFuture()
        testPastTransitive()
        testPresentParticiple()
        testPastParticiple()
        testUshyUshiForm()
        testPresentContinuous()
    }
}