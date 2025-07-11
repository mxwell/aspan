package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import org.slf4j.LoggerFactory

/**
 * Usage:
 *   $ ./gradlew :test:run --args="Adj"
 */
class AdjTest() {
    private val LOG = LoggerFactory.getLogger(this.javaClass)

    private fun checkFormString(expected: String, phrasal: Phrasal, ctx: String? = null) {
        val actual = phrasal.raw
        if (expected != actual) {
            val context = ctx?.let { " -- ${it}" } ?: ""
            LOG.warn("unexpected form: ${actual} instead of ${expected}${context}")
        }
    }

    private fun testRak() {
        val relations = listOf<Pair<String, String>>(
            Pair("таза", "тазарақ"),
            Pair("жаман", "жаманырақ"),
            Pair("жуан", "жуанырақ"),
            Pair("төмен", "төменірек"),
            Pair("биік", "биігірек"),
            Pair("ақ", "ағырақ"),
            Pair("көп", "көбірек"),
            Pair("толық", "толығырақ"),
        )
        for ((adj, rakForm) in relations) {
            checkFormString(
                rakForm,
                AdjBuilder(adj).rakForm()
            )
        }
    }

    private fun testDau() {
        val relations = listOf<Pair<String, String>>(
            Pair("таза", "тазалау"),
            Pair("тәтті", "тәттілеу"),
            Pair("лас", "ластау"),
            Pair("жаман", "жамандау"),
            Pair("жуан", "жуандау"),
            Pair("төмен", "төмендеу"),
            Pair("биік", "биіктеу"),
            Pair("ақ", "ақтау"),
            Pair("көп", "көптеу"),
            Pair("семiз", "семiздеу"),
            Pair("қымбат", "қымбаттау"),
        )
        for ((adj, dauForm) in relations) {
            checkFormString(
                dauForm,
                AdjBuilder(adj).dauForm()
            )
        }
    }

    fun test() {
        testRak()
        testDau()
    }
}