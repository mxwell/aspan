package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import org.slf4j.LoggerFactory

/**
 * Usage:
 *   $ ./gradlew :test:run --args="Declension"
 */
class DeclensionTest() {
    private val LOG = LoggerFactory.getLogger(this.javaClass)

    private fun checkFormString(expected: String, phrasal: Phrasal, ctx: String? = null) {
        val actual = phrasal.raw
        if (expected != actual) {
            val context = ctx?.let { " -- ${it}" } ?: ""
            LOG.warn("unexpected form: ${actual} instead of ${expected}${context}")
        }
    }

    private fun testPresentParticiple() {
        val relations = mapOf<String, List<String>>(
            "келу" to listOf("келетін", "келетіннің", "келетінге", "келетінді", "келетінде", "келетіннен", "келетінмен"),
            "жорғалау" to listOf(
                "жорғалайтын",
                "жорғалайтынның",
                "жорғалайтынға",
                "жорғалайтынды",
                "жорғалайтында",
                "жорғалайтыннан",
                "жорғалайтынмен",
            )
        )
        for ((verb, expectedForms) in relations) {
            val builder = VerbBuilder(verb)
            val nounBuilder = NounBuilder.ofPhrasalBuilder(
                builder.presentParticipleBuilder(SentenceType.Statement),
                builder.extractSoftOffset(),
            )
            for (septikIndex in 0..6) {
                val septik = Septik.ofIndex(septikIndex)
                val septikForm = nounBuilder.septikForm(septik)
                checkFormString(
                    expectedForms[septikIndex],
                    septikForm,
                    "verb ${verb}, septik ${septik}"
                )
            }
        }

    }

    fun test() {
        testPresentParticiple()
    }
}