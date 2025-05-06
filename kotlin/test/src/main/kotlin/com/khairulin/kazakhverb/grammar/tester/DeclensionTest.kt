package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Usage:
 *   $ ./gradlew :test:run --args="Declension /home/murat/git/aspan/data/dict/noun.detect_suggest_forms.20250204.jsonl"
 */

class DeclensionTest(val resourcePath: String) {
    val LOG = LoggerFactory.getLogger(this.javaClass)
    val jsonDecoder = Json {
        ignoreUnknownKeys = true
    }

    private var checkMatchCounter = 0

    fun checkMatch(form: NounForm, phrasal: Phrasal) {
        val expected = form.form
        val actual = phrasal.raw.lowercase()
        if (expected != actual) {
            LOG.warn("unexpected form: ${actual} instead of ${expected}")
        }
        checkMatchCounter += 1
    }

    fun test() {
        val inputStream = File(resourcePath).inputStream()

        var lineIndex = -1
        var baseCounter = 0
        for (line in inputStream.bufferedReader().lines()) {
            lineIndex += 1
            if (lineIndex % 1000 == 0) {
                LOG.info("handling line ${lineIndex}")
            }

            val row = try {
                jsonDecoder.decodeFromString<NounFormsRow>(line)
            } catch (e: Exception) {
                LOG.error("failed to decode line", e)
                break
            }
            val builder = NounBuilder.ofNoun(row.base)
            val savedCounter = checkMatchCounter
            for (form in row.forms) {
                if (form.septik == null) continue
                val septik = Septik.ofIndex(form.septik)
                if (septik == Septik.Tabys) {
                    if (form.number == 0) { // singular
                        if (form.possPerson != null) {
                            val possPerson = GrammarPerson.ofIndex(form.possPerson)
                            if (form.possNumber != null) {
                                val possNumber = GrammarNumber.ofIndex(form.possNumber)
                                val phrasal = builder.possessiveSeptikForm(
                                    possPerson,
                                    possNumber,
                                    septik
                                )
                                checkMatch(form, phrasal)
                            }
                        } else if (form.possNumber == null) {
                            val phrasal = builder.septikForm(Septik.Tabys)
                            checkMatch(form, phrasal)
                        }
                    }
                }
            }
            if (savedCounter != checkMatchCounter) {
                baseCounter += 1
            }
        }

        LOG.info("Checked ${checkMatchCounter} forms for ${baseCounter} words")
    }
}

