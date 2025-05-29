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

    fun test() {
        testJazdau()
        testPastUncertain()
    }
}