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

    private var baseCounter = 0
    private var checkMatchCounter = 0

    private fun checkFormString(expected: String, phrasal: Phrasal, caseSensitive: Boolean) {
        val actual = if (caseSensitive) {
            phrasal.raw
        } else {
            phrasal.raw.lowercase()
        }
        if (expected != actual) {
            LOG.warn("unexpected form: ${actual} instead of ${expected}")
        }
        checkMatchCounter += 1
    }

    private fun checkMatch(form: NounForm, phrasal: Phrasal) {
        checkFormString(form.form, phrasal, caseSensitive = false)
    }

    fun testSpecialCases() {
        val relations = mapOf(
            "ауыз" to listOf(listOf("аузы", "ауызы"), listOf("аузым", "ауызым"), listOf("ауыздары")),
            "АУЫЗ" to listOf(listOf("АУЗы", "АУЫЗы"), listOf("АУЗым", "АУЫЗым"), listOf("АУЫЗдары")),
            "әріп" to listOf(listOf("әрпі"), listOf("әрпім"), listOf("әріптері")),
            "бөрік" to listOf(listOf("бөркі"), listOf("бөркім"), listOf("бөріктері")),
            "ғұрып" to listOf(listOf("ғұрпы"), listOf("ғұрпым"), listOf("ғұрыптары")),
            "дауыс" to listOf(listOf("даусы", "дауысы"), listOf("даусым", "дауысым"), listOf("дауыстары")),
            "дәріп" to listOf(listOf("дәрпі"), listOf("дәрпім"), listOf("дәріптері")),
            "ерік" to listOf(listOf("еркі"), listOf("еркім"), listOf("еріктері")),
            "ерін" to listOf(listOf("ерні"), listOf("ернім"), listOf("еріндері")),
            "зауық" to listOf(listOf("зауқы"), listOf("зауқым"), listOf("зауықтары")),
            "кейіп" to listOf(listOf("кейпі"), listOf("кейпім"), listOf("кейіптері")),
            "көрік" to listOf(listOf("көркі", "көрігі"), listOf("көркім", "көрігім"), listOf("көріктері")),
            "қалып" to listOf(listOf("қалпы", "қалыбы"), listOf("қалпым", "қалыбым"), listOf("қалыптары")),
            "қарын" to listOf(listOf("қарны", "қарыны"), listOf("қарным", "қарыным"), listOf("қарындары")),
            "қаріп" to listOf(listOf("қарпі"), listOf("қарпім"), listOf("қаріптері")),
            "қауіп" to listOf(listOf("қаупі"), listOf("қаупім"), listOf("қауіптері")),
            "қойын" to listOf(listOf("қойны", "қойыны"), listOf("қойным", "қойыным"), listOf("қойындары")),
            "құлық" to listOf(listOf("құлқы"), listOf("құлқым"), listOf("құлықтары")),
            "құлып" to listOf(listOf("құлпы"), listOf("құлпым"), listOf("құлыптары")),
            "мінез-құлық" to listOf(listOf("мінез-құлқы"), listOf("мінез-құлқым"), listOf("мінез-құлықтары")),
            "мойын" to listOf(listOf("мойны"), listOf("мойным"), listOf("мойындары")),
            "мүлік" to listOf(listOf("мүлкі"), listOf("мүлкім"), listOf("мүліктері")),
            "мұрын" to listOf(listOf("мұрны"), listOf("мұрным"), listOf("мұрындары")),
            "нарық" to listOf(listOf("нарқы", "нарығы"), listOf("нарқым", "нарығым"), listOf("нарықтары")),
            "орын" to listOf(listOf("орны"), listOf("орным"), listOf("орындары")),
            "парық" to listOf(listOf("парқы"), listOf("парқым"), listOf("парықтары")),
            "сиық" to listOf(listOf("сиқы"), listOf("сиқым"), listOf("сиықтары")),
            "сұрық" to listOf(listOf("сұрқы"), listOf("сұрқым"), listOf("сұрықтары")),
            "тұрық" to listOf(listOf("тұрқы", "тұрығы"), listOf("тұрқым", "тұрығым"), listOf("тұрықтары")),
            "халық" to listOf(listOf("халқы"), listOf("халқым"), listOf("халықтары")),
            "шырық" to listOf(listOf("шырқы"), listOf("шырқым"), listOf("шырықтары")),
            "ырық" to listOf(listOf("ырқы"), listOf("ырқым"), listOf("ырықтары")),
        )

        for ((noun, forms) in relations) {
            val savedCounter = checkMatchCounter

            val builder = NounBuilder.ofNoun(noun)

            val thirdForms = forms[0]
            val thirdForm = thirdForms[0]
            val thirdResult = builder.possessiveSeptikForm(GrammarPerson.Third, GrammarNumber.Singular, Septik.Atau)
            checkFormString(thirdForm, thirdResult, caseSensitive = true)

            if (thirdForms.size > 1) {
                val altThirdForm = thirdForms[1]
                val alternative = thirdResult.alternative
                if (alternative == null) {
                    LOG.error("alternative form is required for ${noun}")
                } else {
                    checkFormString(altThirdForm, alternative, caseSensitive = true)
                }
            }

            val firstForms = forms[1]
            val firstForm = firstForms[0]
            val firstResult = builder.possessiveSeptikForm(GrammarPerson.First, GrammarNumber.Singular, Septik.Atau)
            checkFormString(firstForm, firstResult, caseSensitive = true)
            if (firstForms.size > 1) {
                val altFirstForm = firstForms[1]
                val alternative = firstResult.alternative
                if (alternative == null) {
                    LOG.error("alternative form is required for ${noun}")
                } else {
                    checkFormString(altFirstForm, alternative, caseSensitive = true)
                }
            }

            val pluralForms = forms[2]
            assert(pluralForms.size == 1)
            val pluralForm = pluralForms[0]
            val pluralResult = builder.possessiveSeptikForm(GrammarPerson.Third, GrammarNumber.Plural, Septik.Atau)
            checkFormString(pluralForm, pluralResult, caseSensitive = true)
            if (pluralResult.alternative != null) {
                LOG.error("unexpected alternative form for plural for ${noun}")
            }

            if (savedCounter != checkMatchCounter) {
                baseCounter += 1
            }
        }
    }

    fun test() {
        testSpecialCases()
        LOG.info("special: checked ${checkMatchCounter} forms")

        val inputStream = File(resourcePath).inputStream()

        var lineIndex = -1
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
                if (form.number != null) {
                    val grammarNumber = GrammarNumber.ofIndex(form.number)
                    if (grammarNumber == GrammarNumber.Singular) {
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
                            val phrasal = builder.septikForm(septik)
                            checkMatch(form, phrasal)
                        }
                    } else {
                        if (form.possPerson == null && form.possNumber == null) {
                            val phrasal = builder.pluralSeptikForm(septik)
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

