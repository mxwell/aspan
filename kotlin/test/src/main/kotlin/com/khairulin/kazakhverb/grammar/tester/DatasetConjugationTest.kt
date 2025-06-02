package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Usage:
 *   $ ./gradlew :test:run --args="DatasetConjugation /data/verb_testset.20241217.jsonl"
 */

class DatasetConjugationTest(val resourcePath: String) {
    val LOG = LoggerFactory.getLogger(this.javaClass)

    private val auxBuilders: Map<TenseIndex, VerbBuilder> by lazy {
        mapOf(
            TenseIndex.presentContinuousJatu to VerbBuilder("жату"),
            TenseIndex.presentContinuousJatuNegateAux to VerbBuilder("жату"),
            TenseIndex.presentContinuousOtyru to VerbBuilder("отыру"),
            TenseIndex.presentContinuousOtyruNegateAux to VerbBuilder("отыру"),
            TenseIndex.presentContinuousTuru to VerbBuilder("тұру"),
            TenseIndex.presentContinuousTuruNegateAux to VerbBuilder("тұру"),
            TenseIndex.presentContinuousJuru to VerbBuilder("жүру"),
            TenseIndex.presentContinuousJuruNegateAux to VerbBuilder("жүру"),
        )
    }

    fun equalForms(
        tenseIndex: TenseIndex,
        sentenceType: SentenceType,
        generated: List<Phrasal>,
        testset: List<Phrasal>
    ): Boolean {
        if (generated.size != testset.size) {
            LOG.error("Form number mismatch: ${generated.size} != ${testset.size} for ${tenseIndex} ${sentenceType}")
            return false
        }

        var result = 1
        for ((generatedForm, testsetForm) in generated.zip(testset)) {
            val g = generatedForm.parts
            val t = testsetForm.parts
            if (g.size != t.size) {
                LOG.error("Phrasal part number mismatch: ${g.size} != ${t.size} for ${tenseIndex} ${sentenceType}, index ${result}")
                return false
            }
            for ((gPart, tPart) in g.zip(t)) {
                if (gPart.content != tPart.content) {
                    LOG.error("Phrasal part mismatch: ${gPart.content} != ${tPart.content} for ${tenseIndex} ${sentenceType}")
                    return false
                }
            }
            result += 1
        }
        return true
    }

    fun generateForms(tenseIndex: TenseIndex, builder: VerbBuilder, sentenceType: SentenceType): List<Phrasal> {
        val result = mutableListOf<Phrasal>()
        for (person in GrammarPerson.entries) {
            for (number in GrammarNumber.entries) {
                when (tenseIndex) {
                    TenseIndex.presentTransitive -> {
                        result.add(builder.presentTransitiveForm(person, number, sentenceType))
                    }
                    TenseIndex.pastSimple -> {
                        result.add(builder.past(person, number, sentenceType))
                    }
                    TenseIndex.conditionalMood -> {
                        result.add(builder.conditionalMood(person, number, sentenceType))
                    }
                    TenseIndex.imperativeMood -> {
                        result.add(builder.imperativeMood(person, number, sentenceType))
                    }
                    TenseIndex.optativeMood -> {
                        result.add(builder.optativeMood(person, number, sentenceType))
                    }
                    TenseIndex.remotePast, TenseIndex.remotePastNegateAux -> {
                        val negateAux = tenseIndex.isNegateAux()
                        result.add(builder.remotePast(person, number, sentenceType, negateAux = negateAux))
                    }
                    else -> {
                        require(tenseIndex.isPresentContinuous()) {
                            "unsupported tense index: ${tenseIndex}"
                        }
                        val auxBuilder = auxBuilders[tenseIndex]!!
                        val negateAux = tenseIndex.isNegateAux()
                        val form = builder.presentContinuousForm(person, number, sentenceType, auxBuilder, negateAux)
                        result.add(form)
                    }
                }
            }
        }
        return result
    }

    fun createBuilder(verb: String, forceExceptional: Boolean): VerbBuilder {
        return VerbBuilder(verb, forceExceptional)
    }

    fun test() {
        val inputStream = File(resourcePath).inputStream()
        var counter = 0
        var ok = true
        inputStream.bufferedReader().forEachLine { line ->
            val row = Json.decodeFromString<TestsetRow>(line)

            val builder = createBuilder(row.verb, row.forceExceptional)

            for (tenseIndex in TenseIndex.entries) {
                for (sentenceType in SentenceType.entries) {
                    val generated = generateForms(tenseIndex, builder, sentenceType)
                    require(generated.isNotEmpty())
                    val testset = row.tenses[tenseIndex.index].getForms(sentenceType)
                    if (!equalForms(tenseIndex, sentenceType, generated, testset)) {
                        ok = false
                        return@forEachLine
                    }
                }
            }

            counter += 1
            if (counter % 1000 == 0) {
                LOG.info("Counter ${counter}")
            }
        }
        LOG.info("Read verbs: ${counter}")
        val testResult = if (ok) "pass" else "fail"
        LOG.info("Test: ${testResult}")
    }
}