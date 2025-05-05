package com.khairulin.kazakhverb.grammar.tester

import com.khairulin.kazakhverb.grammar.NounBuilder
import com.khairulin.kazakhverb.grammar.Septik
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

    fun test() {
        val inputStream = File(resourcePath).inputStream()

        var lineIndex = -1
        for (line in inputStream.bufferedReader().lines()) {
            lineIndex += 1
            if (lineIndex % 100 == 0) {
                LOG.info("handling line ${lineIndex}")
            }

            val row = try {
                jsonDecoder.decodeFromString<NounFormsRow>(line)
            } catch (e: Exception) {
                LOG.error("failed to decode line", e)
                break
            }
            val builder = NounBuilder.ofNoun(row.base)
            for (form in row.forms) {
                if (form.septik == 3) { // Tabys
                    if (form.number == 0) { // singular
                        if (form.possPerson == null && form.possNumber == null) {
                            val phrasal = builder.septikForm(Septik.Tabys)
                            val expected = form.form
                            val actual = phrasal.raw.lowercase()
                            if (expected != actual) {
                                LOG.warn("unexpected form: ${actual} instead of ${expected}")
                            }
                        }
                    }
                }
            }
        }
    }
}

