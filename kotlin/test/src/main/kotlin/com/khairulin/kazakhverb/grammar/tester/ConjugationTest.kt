package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import org.slf4j.LoggerFactory

/**
 * Usage:
 *   $ ./gradlew :test:run --args="Conjugation"
 */
class ConjugationTest() {
    private val LOG = LoggerFactory.getLogger(this.javaClass)

    private fun checkFormString(expected: String, phrasal: Phrasal) {
        val actual = phrasal.raw
        if (expected != actual) {
            LOG.warn("unexpected form: ${actual} instead of ${expected}")
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

    fun test() {
        testJazdau()
    }
}