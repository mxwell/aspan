package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.*
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem
import io.ktor.util.logging.*
import kotlin.random.Random

class TaskGenerator {
    private val LOG = KtorSimpleLogger("TaskGenerator")

    companion object {
        fun collectTranslationsOfList(pairs: List<Pair<String, String>?>): List<List<String>> {
            return pairs.filterNotNull().map {
                listOf(it.first, it.second)
            }
        }
    }

    private val usedForms = listOf(
        GrammarForm.MEN,
        GrammarForm.BIZ,
        GrammarForm.SEN,
        GrammarForm.SIZ,
        GrammarForm.OL,
        GrammarForm.OLAR,
    )

    private val kTaskCount = 10

    private val jatuBuilder: VerbBuilder by lazy {
        VerbBuilder("жату")
    }

    private fun buildSentenceStart(subject: String, objectWord: String): String {
        val obj = if (objectWord.isEmpty()) {
            " "
        } else {
            " ${objectWord} "
        }
        return "${subject}${obj}"
    }

    private fun buildConjugationHint(tense: String, sentenceType: SentenceType): String {
        val sentenceTypeHint = when (sentenceType) {
            SentenceType.Negative -> ", *отрицание*"
            SentenceType.Question -> ", *вопрос*"
            else -> ""
        }
        return "${tense}${sentenceTypeHint}"
    }

    private fun buildTaskDescription(
        tense: String,
        sentenceStart: String,
        verb: String,
        forceExceptional: Boolean,
        sentenceType: SentenceType = SentenceType.Statement,
        subject: String? = null,
        auxVerb: String? = null
    ): String {
        val sb = StringBuilder()
        val hint = buildConjugationHint(tense, sentenceType)
        sb.append("(${hint})\n")
        val subjectPart = if (subject != null) {
            "[${subject}] "
        } else {
            ""
        }
        val noteOnException = if (forceExceptional) {
            ", глагол-исключение"
        } else {
            ""
        }
        val auxVerbPart = if (auxVerb != null) {
            ", вспомогательный глагол ${auxVerb}"
        } else {
            ""
        }
        val questionMark = if (sentenceType == SentenceType.Question) {
            "?"
        } else {
            ""
        }
        sb.append("`${sentenceStart}${subjectPart}[${verb}${noteOnException}${auxVerbPart}]${questionMark}`\n")
        return sb.toString()
    }

    private fun buildTaskDescription(
        tense: String,
        sentenceStart: String,
        verb: VerbInfo,
        sentenceType: SentenceType = SentenceType.Statement,
        subject: String? = null,
        auxVerb: String? = null
    ) = buildTaskDescription(
        tense,
        sentenceStart,
        verb.verb,
        verb.forceExceptional,
        sentenceType,
        subject,
        auxVerb
    )

    data class Combo(
        val taskId: Int,
        val verb: VerbList.Entry,
        val grammarForm: GrammarForm,
        val sentenceType: SentenceType,
    )

    private fun collectTasks(generator: (taskId: Int) -> TaskItem): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..kTaskCount) {
            tasks.add(generator(taskId))
        }
        return GetTasks(tasks)
    }

    private fun generateCombos(pattern: SentenceTypePattern, taskCount: Int): List<Combo> {
        val used = mutableSetOf<String>()
        val result = mutableListOf<Combo>()
        for (taskId in 1..taskCount) {
            var verbEntry = VerbList.entries.random()
            var grammarForm = usedForms.random()
            for (retry in 1..5) {
                val key = "${verbEntry.verb.verb} + ${grammarForm.pronoun}"
                if (used.contains(key)) {
                    verbEntry = VerbList.entries.random()
                    grammarForm = usedForms.random()
                    continue
                } else {
                    used.add(key)
                    break
                }
            }
            val sentenceType = pattern.getSentenceTypeByTaskId(taskId)
            result.add(Combo(taskId, verbEntry, grammarForm, sentenceType))
        }
        return result
    }

    private fun genCommon(pattern: SentenceTypePattern, generator: (combo: Combo) -> TaskItem): GetTasks {
        val combos = generateCombos(pattern, kTaskCount)
        return collectTasks { taskId ->
            generator(combos[taskId - 1])
        }
    }

    private fun buildSupplementForm(grammarForm: GrammarForm, supplementNouns: List<SupplementNoun>): Pair<SupplementNoun, String>? {
        if (supplementNouns.isEmpty()) {
            return null
        }
        for (i in 0..2) {
            val supplement = supplementNouns.random()
            val form = supplement.form(grammarForm)
            if (form == null) {
                LOG.info("buildSupplementForm: bad form for entry supplement ${supplement}, grammarForm ${grammarForm.name}")
                continue
            }
            return Pair(supplement, form)
        }
        return null
    }

    private fun presentTransitiveGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val description = buildTaskDescription(
            "переходное время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genPresentTransitiveEasy() = genCommon(SentenceTypePattern.S10, generator = this::presentTransitiveGenerator)

    private fun genPresentTransitive() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::presentTransitiveGenerator)

    private fun presentContinuousGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = verb.builder()

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val answers = mutableListOf<String>()
        val phrasal = builder.presentContinuousForm(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
            jatuBuilder
        )
        answers.add("${sentenceStart}${phrasal.raw}")
        if (sentenceType == SentenceType.Negative) {
            val phrasal2 = builder.presentContinuousForm(
                grammarForm.person,
                grammarForm.number,
                sentenceType,
                jatuBuilder,
                negateAux = false
            )
            answers.add("${sentenceStart}${phrasal2.raw}")
        }

        val description = buildTaskDescription(
            "настоящее время",
            sentenceStart,
            verb,
            sentenceType,
            auxVerb = "жату",
        )
        return TaskItem(
            description,
            answers,
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genPresentContinuousEasy() = genCommon(SentenceTypePattern.S10, generator = this::presentContinuousGenerator)

    private fun genPresentContinuous() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::presentContinuousGenerator)

    private fun pastGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().past(grammarForm.person, grammarForm.number, sentenceType)

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val description = buildTaskDescription(
            "прошедшее время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genPastEasy() = genCommon(SentenceTypePattern.S10, generator = this::pastGenerator)

    private fun genPast() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::pastGenerator)

    private fun remotePastGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = verb.builder()

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val answers = mutableListOf<String>()
        val phrasal = builder.remotePast(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )
        answers.add("${sentenceStart}${phrasal.raw}")
        if (sentenceType == SentenceType.Negative) {
            val phrasal2 = builder.remotePast(
                grammarForm.person,
                grammarForm.number,
                sentenceType,
                negateAux = false
            )
            answers.add("${sentenceStart}${phrasal2.raw}")
        }

        val description = buildTaskDescription(
            "давнопрошедшее очевидное время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            answers,
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genRemotePastEasy() = genCommon(SentenceTypePattern.S10, generator = this::remotePastGenerator)

    private fun genRemotePast() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::remotePastGenerator)

    private fun pastUncertainGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = verb.builder()

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val answers = mutableListOf<String>()
        val phrasal = builder.pastUncertainTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )
        answers.add("${sentenceStart}${phrasal.raw}")

        val description = buildTaskDescription(
            "давнопрошедшее неочевидное время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            answers,
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genPastUncertainEasy() = genCommon(SentenceTypePattern.S10, generator = this::pastUncertainGenerator)

    private fun genPastUncertain() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::pastUncertainGenerator)

    private fun optativeMoodGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().optativeMood(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.poss, supPair?.second ?: "")

        val description = buildTaskDescription(
            "желательное наклонение",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun optativeMoodPastTenseGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().optativeMoodInPastTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.poss, supPair?.second ?: "")

        val description = buildTaskDescription(
            "желательное наклонение, прошедшее время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genOptativeMoodEasy() = genCommon(SentenceTypePattern.S10, generator = this::optativeMoodGenerator)

    private fun genOptativeMood() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::optativeMoodGenerator)

    private fun genOptativeMoodPastTense() = genCommon(SentenceTypePattern.S10, generator = this::optativeMoodPastTenseGenerator)

    enum class GrammarFormAffinity {
        unspecified,
        matchRequired,  // use the same person+number in both clauses
        mismatchRequired,  // use different person in both clauses
    }

    fun getRandomGrammarFormPair(affinity: GrammarFormAffinity): Pair<GrammarForm, GrammarForm> {
        return when (affinity) {
            GrammarFormAffinity.matchRequired -> {
                val form = usedForms.random()
                Pair(form, form)
            }
            GrammarFormAffinity.mismatchRequired -> {
                val first = usedForms.random()
                var second = first
                for (iter in 1..10) {
                    second = usedForms.random()
                    if (second.person.personPosition != first.person.personPosition) {
                        break
                    }
                }
                if (first.person == second.person) {
                    throw IllegalStateException("failed to generate grammar forms with different persons")
                }
                Pair(first, second)
            }
            else -> {
                Pair(usedForms.random(), usedForms.random())
            }
        }
    }

    data class ConditionalCombo(
        val affinity: GrammarFormAffinity,
        val firstVerb: VerbInfo,
        val secondVerb: VerbInfo,
        val firstSupplements: List<SupplementNoun> = emptyList(),
        val secondSupplements: List<SupplementNoun> = emptyList(),
        val firstSentence: SentenceType = SentenceType.Statement,
        val secondSentence: SentenceType = SentenceType.Statement,
    ) {
        fun collectComboTranslations(): List<List<String>> {
            val pairs = mutableListOf<Pair<String, String>?>()
            for (sup in firstSupplements) {
                pairs.add(sup.asPair())
            }
            pairs.add(firstVerb.asPair())
            for (sup in secondSupplements) {
                pairs.add(sup.asPair())
            }
            pairs.add(secondVerb.asPair())
            return collectTranslationsOfList(pairs)
        }
    }

    private val kConditionalCombos = listOf(
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("алу", translation = "взять"),
            VerbInfo("қайту", translation = "возвращаться"),
            firstSupplements = listOf(
                SupplementNoun.notNoun("бүгін", "сегодня"),
                SupplementNoun("билет", "билет", Septik.Tabys),
            ),
            secondSupplements = listOf(
                SupplementNoun.notNoun("ертең", "завтра"),
                SupplementNoun("үй", "дом", Septik.Barys),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("түсіндіру", translation = "объяснять"),
            VerbInfo("білу", translation = "знать"),
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("тырысу", translation = "стараться"),
            VerbInfo("орындау", translation = "выполнить"),
            secondSupplements = listOf(
                SupplementNoun("тапсырма", "задание", Septik.Tabys),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("шақыру", translation = "приглашать"),
            VerbInfo("бару", translation = "идти"),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("шақыру", translation = "приглашать"),
            VerbInfo("бару", translation = "идти"),
            firstSentence = SentenceType.Negative,
            secondSentence = SentenceType.Negative,
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("оқу", translation = "читать"),
            VerbInfo("түсіну", translation = "понимать"),
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("шаршау", translation = "уставать"),
            VerbInfo("демалу", translation = "отдыхать"),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("сұрау", translation = "спрашивать"),
            VerbInfo("беру", translation = "давать"),
            secondSupplements = listOf(
                SupplementNoun("жауап", "ответ", Septik.Atau),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("ұмыту", translation = "забывать"),
            VerbInfo("телефон соғу", translation = "звонить"),
            firstSentence = SentenceType.Negative,
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("түсіну", translation = "понимать"),
            VerbInfo("көмектесу", translation = "помогать"),
            firstSupplements = listOf(
                SupplementNoun("сабақ", "урок", Septik.Tabys),
                SupplementNoun.notNoun("жақсы", "хорошо"),
            ),
            firstSentence = SentenceType.Negative,
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("тұру", translation = "вставать"),
            VerbInfo("келу", translation = "приезжать"),
            firstSupplements = listOf(
                SupplementNoun.notNoun("ерте", "рано"),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("бару", translation = "идти"),
            VerbInfo("бару", translation = "идти"),
            secondSupplements = listOf(
                SupplementNoun.notNoun("де", "тоже"),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("келу", translation = "приезжать"),
            VerbInfo("ренжу", translation = "обижаться"),
            firstSupplements = listOf(
                SupplementNoun("қонақ", "гость", Septik.Barys),
            ),
            firstSentence = SentenceType.Negative,
        ),
        ConditionalCombo(
            GrammarFormAffinity.mismatchRequired,
            VerbInfo("қарау", translation = "смотреть"),
            VerbInfo("барып келу", translation = "сходить"),
            firstSupplements = listOf(
                SupplementNoun("бала", "ребёнок", Septik.Barys),
            ),
            secondSupplements = listOf(
                SupplementNoun("дүкен", "магазин", Septik.Barys),
            ),
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("асығу", translation = "спешить"),
            VerbInfo("кешігу", translation = "опаздывать"),
            firstSentence = SentenceType.Negative,
        ),
        ConditionalCombo(
            GrammarFormAffinity.matchRequired,
            VerbInfo("асығу", translation = "спешить"),
            VerbInfo("кешігу", translation = "опаздывать"),
            firstSentence = SentenceType.Negative,
            secondSupplements = listOf(
                SupplementNoun("жұмыс", "работа", Septik.Barys),
            ),
        ),
    )

    private fun makeSupplements(supplements: List<SupplementNoun>, subject: GrammarForm): String {
        if (supplements.isEmpty()) {
            return ""
        }
        val parts = mutableListOf<String>()
        for (sup in supplements) {
            parts.add(sup.form(subject)!!)
        }
        return parts.joinToString(" ") + " "
    }

    private fun genConditionalMood() = collectTasks {
        val combo = kConditionalCombos.random()
        val (first, second) = getRandomGrammarFormPair(combo.affinity)

        val ifPrefix = if (Random.nextBoolean()) {
            "егер "
        } else {
            ""
        }
        val firstSupplements = makeSupplements(combo.firstSupplements, first)
        val firstVerbForm = combo
            .firstVerb
            .builder()
            .conditionalMood(first.person, first.number, combo.firstSentence)
            .raw
        val firstStart = "${ifPrefix}${first.pronoun} ${firstSupplements}"
        val secondPronoun = if (first == second && Random.nextBoolean()) "" else "${second.pronoun} "
        val secondSupplements = makeSupplements(combo.secondSupplements, second)
        val secondStart = "${secondPronoun}${secondSupplements}"
        val secondVerbForm = combo
            .secondVerb
            .builder()
            .presentTransitiveForm(second.person, second.number, combo.secondSentence)
            .raw

        val descriptionSb = StringBuilder("(")
        descriptionSb.append("условное наклонение")
        if (combo.firstSentence == SentenceType.Negative) {
            descriptionSb.append(", *отрицание*")
        }
        descriptionSb.append(", ${first.ruShort}")
        descriptionSb.append(" + ")
        descriptionSb.append("переходное время")
        if (combo.secondSentence == SentenceType.Negative) {
            descriptionSb.append(", *отрицание*")
        }
        descriptionSb.append(", ${second.ruShort}")
        descriptionSb.append(")\n")
        descriptionSb.append("`")
        descriptionSb.append(firstStart)
        descriptionSb.append("[${combo.firstVerb.verb}]")
        descriptionSb.append(", ")
        descriptionSb.append(secondStart)
        descriptionSb.append("[${combo.secondVerb.verb}]")
        descriptionSb.append("`")

        val answer = "${firstStart}${firstVerbForm}, ${secondStart}${secondVerbForm}"

        TaskItem(
            descriptionSb.toString(),
            listOf(answer),
            translations = combo.collectComboTranslations()
        )
    }

    private fun canClauseGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().canClause(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val description = buildTaskDescription(
            "конструкция с алу = мочь",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun canClausePastGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().canClauseInPastTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val description = buildTaskDescription(
            "конструкция с алу = мочь, прошедшее время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genCanClauseEasy() = genCommon(SentenceTypePattern.S10, generator = this::canClauseGenerator)

    private fun genCanClause() = genCommon(SentenceTypePattern.S6_N2_Q2, generator = this::canClauseGenerator)

    private fun genCanClausePastTense() = genCommon(SentenceTypePattern.S10, generator = this::canClausePastGenerator)

    private val kLikableSubjects = listOf(
        Pair("кітап", "кітапты"),
        Pair("гүлдер", "гүлдерді"),
        Pair("спортпен айналысу", "спортпен айналысуды"),
        Pair("спортпен айналысқан", "спортпен айналысқанды"),
        Pair("демалысты жоспарлау", "демалысты жоспарлауды"),
        Pair("демалысты жоспарлаған", "демалысты жоспарлағанды"),
        Pair("кешкі ас жасау", "кешкі ас жасауды"),
        Pair("кешкі ас жасаған", "кешкі ас жасағанды"),
        Pair("көлік жүргізу", "көлік жүргізуді"),
        Pair("көлік жүргізген", "көлік жүргізгенді"),
        Pair("дүкен", "дүкенді"),
        Pair("автобус күту", "автобус күтуді"),
        Pair("хатты жазу", "хатты жазуды"),
        Pair("хатты жазған", "хатты жазғанды"),
        Pair("көшеде тұру", "көшеде тұруды"),
    )

    private fun genUnauClause(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..kTaskCount) {
            val grammarForm = usedForms.random()
            val subject = kLikableSubjects.random()
            val negation = Random.nextBoolean()
            val hint = if (negation) {
                "не нравится, переходное время"
            } else {
                "нравится, переходное время"
            }
            val pronoun = grammarForm.pronoun
            val description = "(${hint})\n`[${pronoun}] ${subject.first} [ұнау]`"
            val dative = grammarForm.dative
            val verb = if (negation) "ұнамайды" else "ұнайды"
            val answer = "${dative} ${subject.first} ${verb}"
            tasks.add(TaskItem(description, listOf(answer)))
        }
        return GetTasks(tasks)
    }

    private fun genUnatuClause(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        val verb = "ұнату"
        val builder = VerbBuilder(verb, false)
        for (taskId in 1..kTaskCount) {
            val grammarForm = usedForms.random()
            val subject = kLikableSubjects.random()
            val sentenceType = SentenceTypePattern.S6_N2_Q2.getSentenceTypeByTaskId(taskId)
            val sentenceStart = buildSentenceStart(grammarForm.pronoun, "")
            val description = buildTaskDescription(
                "нравится, переходное время",
                sentenceStart,
                verb,
                false,
                sentenceType,
                subject = subject.first,
            )
            val subjectPart = "${subject.second} "
            val phrasal = builder.presentTransitiveForm(
                grammarForm.person,
                grammarForm.number,
                sentenceType
            )
            tasks.add(TaskItem(
                description,
                listOf("${sentenceStart}${subjectPart}${phrasal.raw}")
            ))
        }
        return GetTasks(tasks)
    }

    private fun genKoruClause(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        val loveVerb = "жақсы көру"
        val hateVerb = "жек көру"
        val loveBuilder = VerbBuilder(loveVerb, false)
        val hateBuilder = VerbBuilder(hateVerb, false)
        for (taskId in 1..kTaskCount) {
            val grammarForm = usedForms.random()
            val subject = kLikableSubjects.random()
            val negation = Random.nextBoolean()
            val hint = if (negation) {
                "ненавидеть, переходное время"
            } else {
                "любить, переходное время"
            }
            val sentenceStart = buildSentenceStart(grammarForm.pronoun, "")
            val description = buildTaskDescription(
                hint,
                sentenceStart,
                if (negation) hateVerb else loveVerb,
                false,
                SentenceType.Statement,
                subject = subject.first
            )
            val subjectPart = "${subject.second} "
            val phrasal = if (negation) {
                hateBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)
            } else {
                loveBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)
            }
            tasks.add(TaskItem(
                description,
                listOf("${sentenceStart}${subjectPart}${phrasal.raw}")
            ))
        }
        return GetTasks(tasks)
    }

    private fun tryClauseEasyGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType

        val phrasal = verb.builder().koruClauseOfTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
            VerbTense.TensePresentTransitive,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.pronoun, supPair?.second ?: "")

        val description = buildTaskDescription(
            "конструкция с көру - попробовать, переходное время",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genTryClauseEasy() = genCommon(SentenceTypePattern.S7_Q3, this::tryClauseEasyGenerator)

    private val kTryClauseTenseVariants = listOf(
        VerbTense.TensePresentTransitive,
        VerbTense.TensePast,
        VerbTense.MoodOptative,
    )

    private fun tryClauseGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val tense = kTryClauseTenseVariants.random()

        val phrasal = verb.builder().koruClauseOfTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
            tense,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.getPronounByTense(tense), supPair?.second ?: "")

        val description = buildTaskDescription(
            "конструкция с көру - попробовать, ${tense.ruName}",
            sentenceStart,
            verb,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}"),
            collectTranslations(
                supPair?.first?.asPair(),
                verb.asPair(),
            )
        )
    }

    private fun genTryClause() = genCommon(SentenceTypePattern.S7_Q3, this::tryClauseGenerator)

    data class JazdauCombo(
        val supplement: SupplementNoun?,
        val verb: VerbInfo,
        val auxVerb: String,
    )

    private val kJazdauCombos = listOf(
        JazdauCombo(null, VerbInfo("ұмыту", false, "забывать"), "қалу"),
        JazdauCombo(null, VerbInfo("айту", false, "сказать"), "салу"),
        JazdauCombo(null, VerbInfo("жоғалту", false, "терять"), "алу"),
        JazdauCombo(null, VerbInfo("кешігу", false, "опаздывать"), "қалу"),
        JazdauCombo(SupplementNoun("олармен", "с ними", null), VerbInfo("кездесу", false, "встречаться"), "қалу"),
        JazdauCombo(SupplementNoun("көлік", "транспорт", Septik.Barys), VerbInfo("соғылу", false, "ударить"), "қалу"),
        JazdauCombo(SupplementNoun("емтихан", "экзамен", Septik.Shygys), VerbInfo("құлау", false, "проваливаться"), "қалу"),
        JazdauCombo(SupplementNoun("шындық", "правда", Septik.Tabys), VerbInfo("айту", false, "сказать"), "қою"),
        JazdauCombo(SupplementNoun("ұшақ", "самолёт", Septik.Barys), VerbInfo("қалу", false, "отставать"), "қою"),
        JazdauCombo(SupplementNoun("телефон", "телефон", Septik.Tabys), VerbInfo("түсіру", false, "ронять"), "алу"),
        JazdauCombo(SupplementNoun("ашу", "злость", Septik.Shygys), VerbInfo("жылау", false, "плакать"), "қалу"),
        JazdauCombo(SupplementNoun("ауыл", "аул", Septik.Jatys), VerbInfo("адасу", false, "плутать"), "кету"),
        JazdauCombo(null, VerbInfo("есінен тану", false, "падать в обморок"), "қалу"),
        JazdauCombo(SupplementNoun("үй", "дом", Septik.Shygys), VerbInfo("кету", false, "отправляться"), "қалу"),
        JazdauCombo(SupplementNoun("кілт", "ключ", Septik.Tabys), VerbInfo("жоғалту", false, "терять"), "қалу"),
        JazdauCombo(SupplementNoun("самса", "самса", Septik.Tabys), VerbInfo("беру", false, "давать"), "қою"),
        JazdauCombo(null, VerbInfo("өлу", false, "умирать"), "қалу"),
        JazdauCombo(SupplementNoun("апан", "яма", Septik.Barys), VerbInfo("кіру", false, "вступать"), "кету"),
        JazdauCombo(SupplementNoun("қолайсыз жағдай", "неловкое положение", Septik.Barys), VerbInfo("ұшырау", false, "попадать"), "қалу"),
        JazdauCombo(SupplementNoun("оқ", "пуля", Septik.Barys), VerbInfo("арандау", false, "оказаться в опасности"), "қалу"),
        JazdauCombo(SupplementNoun("көйлек", "рубашка", Septik.Tabys), VerbInfo("жырту", false, "рвать"), "алу"),
    )

    private fun genJazdauClause() = collectTasks {
        val grammarForm = usedForms.random()
        val combo = kJazdauCombos.random()
        val startSb = StringBuilder(grammarForm.pronoun)
        if (combo.supplement != null) {
            startSb.append(" ")
            startSb.append(combo.supplement.form(grammarForm))
        }
        val sentenceStart = startSb.toString()
        val description =
            "(действие чуть не совершилось)\n`${sentenceStart} [${combo.verb.verb} + ${combo.auxVerb} + жаздау]`"
        val auxBuilder = VerbBuilder(combo.auxVerb)
        val verbForm = combo
            .verb.builder()
            .jazdauClause(grammarForm.person, grammarForm.number, auxBuilder)
            .raw
        val answer = "${sentenceStart} ${verbForm}"

        TaskItem(
            description,
            listOf(answer),
            translations = collectTranslations(
                combo.supplement?.asPair(),
                combo.verb.asPair()
            )
        )
    }

    data class ConjunctiveCombo(
        val affinity: GrammarFormAffinity,
        val firstVerb: VerbInfo,
        val secondVerb: VerbInfo,
        val first3pSubject: SupplementNoun? = null,
        val second3pSubject: SupplementNoun? = null,
        val firstSupplements: List<SupplementNoun> = emptyList(),
        val secondSupplements: List<SupplementNoun> = emptyList(),
    )

    private fun getArForm(verb: String, sentenceType: SentenceType): String {
        return VerbBuilder(verb)
            .possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, sentenceType)
            .raw
    }

    private fun getEdiForm(grammarForm: GrammarForm): String {
        return VerbBuilder("еу")
            .past(grammarForm.person, grammarForm.number, SentenceType.Statement)
            .raw
    }

    private fun buildConjunctiveDescription(grammarForm: GrammarForm, template: String) =
        "(сослагательное наклонение с -ар и еді, ${grammarForm.ruShort})\n`${template}`"

    private fun conjunctiveArGen1(): TaskItem {
        val (first, second) = getRandomGrammarFormPair(GrammarFormAffinity.mismatchRequired)
        val firstVerbForm = VerbBuilder("білу")
            .conditionalMood(first.person, first.number, SentenceType.Statement)
            .raw
        val secondBarys = second.dative
        val secondVerbForm = getArForm("айту", SentenceType.Statement)
        val ediForm = getEdiForm(first)
        val sentenceStart = "${first.pronoun} осыны ${firstVerbForm}, ${first.pronoun} ${secondBarys}"
        val template = "${sentenceStart} [айту]"
        val description = buildConjunctiveDescription(first, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("осы", "это"),
                listOf("білу", "знать"),
                listOf("айту", "сказать"),
            )
        )
    }

    private fun conjunctiveArGen2(): TaskItem {
        // мүмкіндігІМ болса, МЕН машина сатып алар едіМ
        val grammarForm = usedForms.random()
        val firstAtau = NounBuilder.ofNoun("мүмкіндік")
            .possessiveSeptikForm(grammarForm.person, grammarForm.number, Septik.Atau)
            .raw
        val firstVerbForm = VerbBuilder("болу")
            .conditionalMood(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement)
            .raw
        val secondVerb = "сатып алу"
        val secondVerbForm = getArForm(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)
        val sentenceStart = "${firstAtau} ${firstVerbForm}, ${grammarForm.pronoun} машина"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(grammarForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("мүмкіндік", "возможность"),
                listOf("болу", "быть"),
                listOf("сатып алу", "купить"),
            )
        )
    }

    private fun conjunctiveArGen3(): TaskItem {
        // МЕН бизнесмен болсаМ, көп ақшаМ болар еді
        val grammarForm = usedForms.random()
        val firstVerbForm = VerbBuilder("болу")
            .conditionalMood(grammarForm.person, grammarForm.number, SentenceType.Statement)
            .raw
        val secondAtauForm = NounBuilder.ofNoun("ақша")
            .possessiveSeptikForm(grammarForm.person, grammarForm.number, Septik.Atau)
            .raw
        val secondVerb = "болу"
        val secondVerbForm = getArForm(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(GrammarForm.OL)

        val sentenceStart = "${grammarForm.pronoun} бизнесмен ${firstVerbForm}, көп ${secondAtauForm}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(GrammarForm.OL, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("болу", "быть"),
                listOf("көп", "много"),
                listOf("ақша", "деньги"),
            ),
        )
    }

    private fun conjunctiveArGen4(): TaskItem {
        // егер МЕНДЕ көп ақша болса, МЕН үй сатып алар едіМ
        val grammarForm = usedForms.random()
        val firstVerbForm = VerbBuilder("болу")
            .conditionalMood(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement)
            .raw
        val secondVerb = "сатып алу"
        val secondVerbForm = getArForm(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.jatys} көп ақша ${firstVerbForm}, ${grammarForm.pronoun} үй"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(GrammarForm.OL, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("көп", "много"),
                listOf("ақша", "деньги"),
                listOf("болу", "быть"),
                listOf("үй", "дом"),
                listOf("сатып алу", "купить"),
            )
        )
    }

    private fun genConjunctiveAr() = collectTasks {
        val range = 10
        val position = Random.nextInt(range * 10)
        if (position < range) {
            conjunctiveArGen1()
        } else if (position < 2 * range) {
            conjunctiveArGen2()
        } else if (position < 3 * range) {
            conjunctiveArGen3()
        } else {
            conjunctiveArGen4()
        }
    }

    private fun buildSeptikDescription(sentenceStart: String, septik: String, objectWord: String, verbForm: String): String {
        val sb = StringBuilder()
        sb.append("(${septik})\n")
        sb.append("`${sentenceStart}[${objectWord}] ${verbForm}`")
        return sb.toString()
    }

    private fun genTabysEasy(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        val combos = listOf<Pair<String, String>>(
            Pair("терезе", "жабу"),
            Pair("дос", "шақыру"),
            Pair("көше", "жөндеу"),
            // FIXME support alternative declension
            // Pair("дауыс", "есту"),
            Pair("есік", "ашу"),
            Pair("есік", "тауып алу"),
            Pair("мектеп", "көрсету"),
            Pair("доп", "қою"),
            Pair("жұмыс", "тексеру"),
            Pair("көктем", "жақсы көру"),
            Pair("мысық", "көру"),
            Pair("қызыл түс", "ұнату"),
            Pair("бұл әдет", "білу"),
            Pair("тұзсыз тамақ", "жек көру"),
            Pair("кофе", "жақсы көру"),
            Pair("кітап", "беру"),
            Pair("ботқа", "пісіру"),
            Pair("алма", "апару"),
            Pair("осы қалам", "алу"),
            Pair("бүгінгі газет", "оқу"),
        )
        for (taskId in 1..kTaskCount) {
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val sentenceStart = buildSentenceStart(grammarForm.pronoun, "")
            val verbBuilder = VerbBuilder(combo.second, forceExceptional = false)
            val sentenceType = if (Random.nextInt() % 4 == 0) SentenceType.Negative else SentenceType.Statement
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, sentenceType).raw
            }
            val description = buildSeptikDescription(sentenceStart, "винительный падеж", combo.first, verbForm)
            val nounBuilder = NounBuilder.ofNoun(combo.first)
            val nounForm = nounBuilder.septikForm(Septik.Tabys).raw
            tasks.add(TaskItem(
                description,
                listOf(
                    "${sentenceStart}${nounForm} ${verbForm}"
                )
            ))
        }
        return GetTasks(tasks)
    }

    /**
     * Return:
     * - two strings if phrasal has an alternative variant,
     * - one string otherwise
     */
    private fun buildAnswers(prefix: String, suffix: String, phrasal: Phrasal): List<String> {
        val result = mutableListOf<String>(
            "${prefix}${phrasal.raw}${suffix}"
        )
        val alternative = phrasal.alternative
        if (alternative != null) {
            result.add(
                "${prefix}${alternative.raw}${suffix}"
            )
        }
        return result
    }

    private fun genTabys(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        val combos = listOf<Pair<SupplementNoun, VerbInfo>>(
            Pair(SupplementNoun("терезе", "окно", null), VerbInfo("жабу", translation = "закрывать")),
            Pair(SupplementNoun("даяшы", "официант", null), VerbInfo("шақыру", translation = "звать")),
            Pair(SupplementNoun("дос", "друг", null), VerbInfo("шақыру", translation = "звать")),
            Pair(SupplementNoun("көше", "улица", null), VerbInfo("жөндеу", translation = "ремонтировать")),
            Pair(SupplementNoun("есік", "дверь", null), VerbInfo("ашу", translation = "открывать")),
            Pair(SupplementNoun("ауыз", "рот", null), VerbInfo("ашу", translation = "открывать")),
            Pair(SupplementNoun("мектеп", "школа", null), VerbInfo("көрсету", translation = "показывать")),
            Pair(SupplementNoun("доп", "мяч", null), VerbInfo("қою", translation = "класть")),
            Pair(SupplementNoun("жұмыс", "работа", null), VerbInfo("тексеру", translation = "проверять")),
            Pair(SupplementNoun("мысық", "кошка", null), VerbInfo("көру", translation = "видеть")),
            Pair(SupplementNoun("кітап", "книга", null), VerbInfo("беру", translation = "давать")),
            Pair(SupplementNoun("алма", "яблоко", null), VerbInfo("апару", translation = "относить")),
            Pair(SupplementNoun("қалам", "ручка", null), VerbInfo("алу", translation = "брать")),
            Pair(SupplementNoun("газет", "газета", null), VerbInfo("оқу", translation = "читать")),
            Pair(SupplementNoun("шабдалы", "персик", null), VerbInfo("әкеліп беру", translation = "приносить")),
        )
        for (taskId in 1..kTaskCount) {
            val grammarForm = usedForms.random()
            val possForm = usedForms.random()
            val objectNumber = if (Random.nextInt() % 5 == 0) GrammarNumber.Plural else GrammarNumber.Singular
            val combo = combos.random()
            val sentenceStart = buildSentenceStart(grammarForm.pronoun, "")
            val verbBuilder = combo.second.builder()
            val sentenceType = if (Random.nextInt() % 4 == 0) SentenceType.Negative else SentenceType.Statement
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, sentenceType).raw
            }
            val formHintSb = StringBuilder()
            formHintSb.append("винительный падеж")
            val objectHintSb = StringBuilder()
            objectHintSb.append(combo.first.noun)
            if (objectNumber == GrammarNumber.Plural) {
                formHintSb.append(", множественное число")
                objectHintSb.append(" ++")
            } else {
                formHintSb.append(", притяжательная форма для ")
                formHintSb.append(possForm.ruShort)
                objectHintSb.append(" ∈ ${possForm.pronoun}")
            }
            val description = buildSeptikDescription(sentenceStart, formHintSb.toString(), objectHintSb.toString(), verbForm)
            val nounBuilder = combo.first.builder()
            val nounForm = if (objectNumber == GrammarNumber.Plural) {
                nounBuilder.pluralSeptikForm(Septik.Tabys)
            } else {
                nounBuilder.possessiveSeptikForm(possForm.person, possForm.number, Septik.Tabys)
            }
            tasks.add(TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    combo.first.asPair(),
                    combo.second.asPair(),
                )
            ))
        }

        return GetTasks(tasks)
    }

    private val kIlikVerbs = listOf(
        "жатқан сияқты",
        "жатыр",
        "болған",
        "болған сияқты",
        "болыпты",
    )

    private val kIlikSubjects = listOf(
        Pair("арқан", "верёвка"),
        Pair("балта", "топор"),
        Pair("жастық", "подушка"),
        Pair("зат", "вещь"),
        Pair("кітап", "книга"),
        Pair("қасық", "ложка"),
        Pair("сабын", "мыло"),
        Pair("сөздік", "словарь"),
        Pair("түйме", "пуговица"),
        Pair("ұялы телефон", "сотовый телефон"),
    )

    private val kIlikPlaces = listOf(
        Pair("бөлме", "комната"),
        Pair("бұрыш", "угол"),
        Pair("жуынатын бөлме", "ванная"),
        Pair("пәтер", "квартира"),
        Pair("сөре", "полка"),
        Pair("төсек", "постель"),
        Pair("үстел", "стол"),
        Pair("шелек", "ведро"),
        Pair("шкаф", "шкаф"),
        Pair("еден", "пол"),
    )

    fun genIlikEasy(): GetTasks {
        val properNames = listOf(
            "Абай",
            "Ажар",
            "Айсылу",
            "Әлия",
            "Аружан",
            "Асель",
            "Ерлан",
            "Жібек",
            "Перизат",
            "Феруза",
        )

        return collectTasks { taskId ->
            val properName = properNames.random()
            val subject = kIlikSubjects.random()
            val place = kIlikPlaces.random()
            val verb = kIlikVerbs.random()

            val properNameForm = NounBuilder.ofNoun(properName).septikForm(Septik.Ilik)
            val subjectForm = NounBuilder.ofNoun(subject.first).possessiveSeptikForm(GrammarPerson.Third, GrammarNumber.Singular, Septik.Atau)
            val placeForm = NounBuilder.ofNoun(place.first).septikForm(Septik.Jatys)

            val sentenceEnd = "${subjectForm.raw} ${placeForm.raw} ${verb}"

            val description = buildSeptikDescription(
                "",
                "родительный падеж",
                properName,
                sentenceEnd,
            )
            TaskItem(
                description,
                buildAnswers("", " ${sentenceEnd}", properNameForm),
                translations = collectTranslations(
                    subject,
                    place
                )
            )
        }
    }

    data class OwnerInfo(
        val noun: String,
        val pluralPossible: Boolean,
        val translation: String,
    ) {
        fun getTranslation() = Pair(noun, translation)
    }

    private val kOwners = listOf(
        OwnerInfo("ата", false, "дед"),
        OwnerInfo("әке", false, "отец"),
        OwnerInfo("әріптес", true, "коллега"),
        OwnerInfo("бастық", false, "начальник"),
        OwnerInfo("дос", true, "друг"),
        OwnerInfo("көрші", true, "сосед"),
        OwnerInfo("қонақ", true, "гость"),
        OwnerInfo("қыз", true, "девочка"),
        OwnerInfo("маман", true, "специалист"),
        OwnerInfo("мұғалім", true, "учитель"),
        OwnerInfo("оқушы", true, "ученик"),
        OwnerInfo("студент", true, "студент"),
    )

    fun genIlik(): GetTasks {
        return collectTasks { taskId ->
            val owner = kOwners.random()
            val subject = kIlikSubjects.random()
            val place = kIlikPlaces.random()
            val verb = kIlikVerbs.random()

            val ownerNumber = if (owner.pluralPossible && Random.nextBoolean()) GrammarNumber.Plural else GrammarNumber.Singular
            val ownerPossForm = usedForms.random()
            val ownerBuilder = NounBuilder.ofNoun(owner.noun)

            val ownerForm = if (ownerNumber == GrammarNumber.Plural) {
                ownerBuilder.pluralSeptikForm(Septik.Ilik)
            } else {
                ownerBuilder.possessiveSeptikForm(ownerPossForm.person, ownerPossForm.number, Septik.Ilik)
            }

            val subjectForm = NounBuilder.ofNoun(subject.first).possessiveSeptikForm(GrammarPerson.Third, GrammarNumber.Singular, Septik.Atau)
            val placeForm = NounBuilder.ofNoun(place.first).septikForm(Septik.Jatys)

            val sentenceEnd = "${subjectForm.raw} ${placeForm.raw} ${verb}"

            val formDescriptionSb = StringBuilder("родительный падеж")
            val formHintSb = StringBuilder(owner.noun)
            if (ownerNumber == GrammarNumber.Plural) {
                formDescriptionSb.append(", множественное число")
                formHintSb.append(" ++")
            } else {
                formDescriptionSb.append(", притяжательная форма для ")
                formDescriptionSb.append(ownerPossForm.ruShort)
                formHintSb.append(" ∈ ${ownerPossForm.pronoun}")
            }

            val description = buildSeptikDescription(
                "",
                formDescriptionSb.toString(),
                formHintSb.toString(),
                sentenceEnd,
            )
            TaskItem(
                description,
                buildAnswers("", " ${sentenceEnd}", ownerForm),
                translations = collectTranslations(
                    owner.getTranslation(),
                    subject,
                    place
                )
            )
        }
    }

    data class NounInfo(
        val noun: String,
        val translation: String,
    ) {
        fun builder() = NounBuilder.ofNoun(noun)
        fun asPair() = Pair(noun, translation)
    }

    private fun makeNounList(vararg nounsAndTranslations: String): List<NounInfo> {
        assert(nounsAndTranslations.size % 2 == 0)

        val result = mutableListOf<NounInfo>()
        for ((noun, translation) in nounsAndTranslations.toList().windowed(2, 2, partialWindows = false)) {
            result.add(NounInfo(noun, translation))
        }
        return result
    }

    private fun makeVerbList(vararg verbs: String): List<VerbInfo> {
        return verbs.map {
            VerbInfo(it, forceExceptional = false)
        }
    }

    private fun makeVerbListWithTranslation(vararg verbsAndTranslations: String): List<VerbInfo> {
        assert(verbsAndTranslations.size % 2 == 0)

        val result = mutableListOf<VerbInfo>()
        for ((noun, translation) in verbsAndTranslations.toList().windowed(2, 2, partialWindows = false)) {
            result.add(VerbInfo(noun, translation = translation))
        }
        return result
    }

    data class BarysCombo(
        val dst: String,
        val dstTranslation: String,
        val verbs: List<VerbInfo>,
    )

    fun genBarysEasy(): GetTasks {
        val regularVerbs = makeVerbList(
            "бару", "келу", "барып келу", "кету", "жету",
        )
        val kiruVerbs = makeVerbList(
            "қайта кіру", "кіру"
        )
        val combos = listOf(
            BarysCombo("ауыл", "аул", regularVerbs),
            BarysCombo("қала", "город", regularVerbs),
            BarysCombo("дүкен", "магазин", regularVerbs + kiruVerbs),
            BarysCombo("үй", "дом", kiruVerbs),
            BarysCombo("жатақхана", "общежитие", regularVerbs + kiruVerbs),
            BarysCombo("бекет", "станция", kiruVerbs),
            BarysCombo("шаштараз", "парикмахерская", regularVerbs + kiruVerbs),
            BarysCombo("дос", "друг", makeVerbList("беру")),
            BarysCombo("тау", "гора", makeVerbList("шығу")),
            BarysCombo("жаттықтырушы", "тренер", makeVerbList("жүру")),
            BarysCombo("мақсат", "цель", makeVerbList("жету")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.dst
            val verb = combo.verbs.random()

            val nounForm = NounBuilder.ofNoun(noun).septikForm(Septik.Barys)
            val verbBuilder = VerbBuilder(verb.verb, verb.forceExceptional)
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            }

            val sentenceStart = "${grammarForm.pronoun} "

            val description = buildSeptikDescription(
                sentenceStart,
                "дательный падеж",
                noun,
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    Pair(noun, combo.dstTranslation)
                )
            )
        }
    }

    fun genBarys(): GetTasks {
        val regularVerbs = makeVerbList(
            "бару", "келу", "барып келу", "кету", "жету",
        )
        val kiruVerbs = makeVerbList(
            "қайта кіру", "кіру"
        )
        val combos = listOf(
            BarysCombo("ауыл", "аул", regularVerbs),
            BarysCombo("қала", "город", regularVerbs),
            BarysCombo("дүкен", "магазин", regularVerbs + kiruVerbs),
            BarysCombo("үй", "дом", kiruVerbs),
            BarysCombo("жатақхана", "общежитие", regularVerbs + kiruVerbs),
            BarysCombo("бекет", "станция", kiruVerbs),
            BarysCombo("шаштараз", "парикмахерская", regularVerbs + kiruVerbs),
            BarysCombo("дос", "друг", makeVerbList("беру")),
            BarysCombo("жаттықтырушы", "тренер", makeVerbList("жүру")),
            BarysCombo("мақсат", "цель", makeVerbList("жету")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.dst
            val verb = combo.verbs.random()
            val possForm = grammarForm

            val nounBuilder = NounBuilder.ofNoun(noun)
            val nounForm = nounBuilder.possessiveSeptikForm(possForm.person, possForm.number, Septik.Barys)
            val verbBuilder = VerbBuilder(verb.verb, verb.forceExceptional)
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            }

            val sentenceStart = "${grammarForm.pronoun} "

            val formDescriptionSb = StringBuilder("дательный падеж для ")
            formDescriptionSb.append(possForm.ruShort)
            val formHintSb = StringBuilder(noun)
            formHintSb.append(" ∈ ${possForm.pronoun}")

            val description = buildSeptikDescription(
                sentenceStart,
                formDescriptionSb.toString(),
                formHintSb.toString(),
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    Pair(noun, combo.dstTranslation)
                )
            )
        }
    }

    private fun genJatysEasy(): GetTasks {
        return collectTasks { taskId ->
            val subject = kIlikSubjects.random()
            val place = kIlikPlaces.random()
            val verb = kIlikVerbs.random()

            val placeForm = NounBuilder.ofNoun(place.first).septikForm(Septik.Jatys)

            val sentenceStart = "${subject.first} "

            val description = buildSeptikDescription(
                sentenceStart,
                "местный падеж",
                place.first,
                verb,
            )
            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verb}", placeForm),
                translations = collectTranslations(
                    subject,
                    place
                )
            )
        }
    }

    private fun genJatys(): GetTasks {
        return collectTasks { taskId ->
            val subject = kIlikSubjects.random()

            val possForm = usedForms.random()
            val place = kIlikPlaces.random()
            val verb = kIlikVerbs.random()

            val placeForm = NounBuilder.ofNoun(place.first).possessiveSeptikForm(possForm.person, possForm.number, Septik.Jatys)

            val sentenceStart = "${subject.first} "
            val formDescription = "местный падеж для ${possForm.ruShort}"
            val formHint = "${place.first} ∈ ${possForm.pronoun}"

            val description = buildSeptikDescription(
                sentenceStart,
                formDescription,
                formHint,
                verb,
            )
            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verb}", placeForm),
                translations = collectTranslations(
                    subject,
                    place
                )
            )
        }
    }

    fun genShygysEasy(): GetTasks {
        val regularVerbs = makeVerbList(
            "бару", "келу", "кету", "жету",
        )
        val combos = listOf(
            BarysCombo("ауыл", "аул", regularVerbs),
            BarysCombo("қала", "город", regularVerbs),
            BarysCombo("дүкен", "магазин", regularVerbs + makeVerbList("сатып алу")),
            BarysCombo("үй", "дом", regularVerbs),
            BarysCombo("жатақхана", "общежитие", regularVerbs),
            BarysCombo("бекет", "станция", regularVerbs),
            BarysCombo("шаштараз", "парикмахерская", regularVerbs),
            BarysCombo("дос", "друг", regularVerbs),
            BarysCombo("тау", "гора", regularVerbs),
            BarysCombo("базар", "базар", regularVerbs + makeVerbList("сатып алу")),
            BarysCombo("мұғалім", "учитель", makeVerbList("сұрау")),
            BarysCombo("бөлме", "комната", makeVerbList("шығу")),
            BarysCombo("жібек", "шёлк", makeVerbList("белдемше тігу")),
            BarysCombo("ит", "собака", makeVerbList("қорқу")),
            BarysCombo("тышқан", "мышь", makeVerbList("қорқу")),
            BarysCombo("дәптер", "тетрадь", makeVerbList("көшіру")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.dst
            val verb = combo.verbs.random()

            val nounForm = NounBuilder.ofNoun(noun).septikForm(Septik.Shygys)
            val verbBuilder = VerbBuilder(verb.verb, verb.forceExceptional)
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, SentenceType.Statement).raw
            }

            val sentenceStart = "${grammarForm.pronoun} "

            val description = buildSeptikDescription(
                sentenceStart,
                "исходный падеж",
                noun,
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    Pair(noun, combo.dstTranslation)
                )
            )
        }
    }

    fun genShygys(): GetTasks {
        val regularVerbs = makeVerbList(
            "бару", "келу", "кету", "жету",
        )
        val combos = listOf(
            BarysCombo("ауыл", "аул", regularVerbs),
            BarysCombo("қала", "город", regularVerbs),
            BarysCombo("дүкен", "магазин", regularVerbs),
            BarysCombo("үй", "дом", regularVerbs),
            BarysCombo("жатақхана", "общежитие", regularVerbs),
            BarysCombo("бекет", "станция", regularVerbs),
            BarysCombo("шаштараз", "парикмахерская", regularVerbs),
            BarysCombo("дос", "друг", regularVerbs),
            BarysCombo("мұғалім", "учитель", makeVerbList("сұрау")),
            BarysCombo("бөлме", "комната", makeVerbList("шығу")),
            BarysCombo("ит", "собака", makeVerbList("қорқу")),
            BarysCombo("тышқан", "мышь", makeVerbList("қорқу")),
            BarysCombo("дәптер", "тетрадь", makeVerbList("көшіру")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.dst
            val verb = combo.verbs.random()
            val possForm = grammarForm

            val nounForm = NounBuilder.ofNoun(noun).possessiveSeptikForm(possForm.person, possForm.number, Septik.Shygys)
            val verbBuilder = VerbBuilder(verb.verb, verb.forceExceptional)
            val sentenceType = if (Random.nextInt() % 4 == 0) SentenceType.Negative else SentenceType.Statement
            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, sentenceType).raw
            }

            val sentenceStart = "${grammarForm.pronoun} "
            val formDescription = "исходный падеж для ${possForm.ruShort}"
            val formHint = "${noun} ∈ ${possForm.pronoun}"

            val description = buildSeptikDescription(
                sentenceStart,
                formDescription,
                formHint,
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    Pair(noun, combo.dstTranslation)
                )
            )
        }
    }

    data class KomektesCombo(
        val tools: List<NounInfo>,
        val verbs: List<VerbInfo>,
        val supplementNoun: SupplementNoun? = null,  // word that goes before tool
    )

    private fun collectTranslations(vararg pairs: Pair<String, String>?): List<List<String>> {
        return collectTranslationsOfList(pairs.toList())
    }

    fun genKomektesEasy(): GetTasks {
        val combos = listOf(
            KomektesCombo(makeNounList("пышақ", "нож"), makeVerbListWithTranslation("кесу", "резать"), SupplementNoun("нан", "хлеб", Septik.Tabys)),
            KomektesCombo(makeNounList("балта", "топор"), makeVerbListWithTranslation("жару", "колоть"), SupplementNoun("отын", "дрова", Septik.Tabys)),
            KomektesCombo(makeNounList("машина", "автомобиль", "пойыз", "поезд", "автобус", "автобус", "ат", "лошадь"), makeVerbListWithTranslation("келу", "приезжать", "бару", "ехать"), SupplementNoun("қала", "город", Septik.Barys)),
            KomektesCombo(makeNounList("ұшақ", "самолёт"), makeVerbListWithTranslation("ұшу", "лететь"), SupplementNoun("астана", "столица", Septik.Barys)),
            KomektesCombo(makeNounList("қасық", "ложка", "шанышқы", "вилка"), makeVerbListWithTranslation("жеу", "кушать"), SupplementNoun("ботқа", "каша", Septik.Tabys)),
            KomektesCombo(makeNounList("қалам", "ручка", "қарындаш", "карандаш", "бор", "мел"), makeVerbListWithTranslation("жазу", "писать")),
            KomektesCombo(makeNounList("көпір", "мост", "жол", "дорога"), makeVerbListWithTranslation("жүру", "идти", "келу", "приходить", "бару", "идти")),
            KomektesCombo(makeNounList("телефон", "телефон"), makeVerbListWithTranslation("сөйлесу", "разговаривать")),
            KomektesCombo(makeNounList("сабын", "мыло"), makeVerbListWithTranslation("жуу", "мыть"), SupplementNoun("қол", "рука", Septik.Tabys)),
            KomektesCombo(makeNounList("сүрткіш", "тряпка"), makeVerbListWithTranslation("сүрту", "вытирать"), SupplementNoun("тақта", "доска", Septik.Tabys)),
            KomektesCombo(makeNounList("лимон", "лимон", "сүт", "молоко", "кесе", "пиала"), makeVerbListWithTranslation("ішу", "пить"), SupplementNoun("шай", "чай", Septik.Tabys)),
            KomektesCombo(makeNounList("лимон", "лимон", "сүт", "молоко"), makeVerbListWithTranslation("шай ішу", "пить чай")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.tools.random()
            val verb = combo.verbs.random()
            val verbBuilder = verb.builder()
            val sentenceType = SentenceType.Statement

            val supForm = combo.supplementNoun?.form(grammarForm)
            val supplement = if (supForm != null) {
                "${supForm} "
            } else {
                ""
            }

            val nounForm = noun.builder().septikForm(Septik.Komektes)

            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, sentenceType).raw
            }

            val sentenceStart = "${grammarForm.pronoun} ${supplement}"
            val formDescription = "творительный падеж"

            val description = buildSeptikDescription(
                sentenceStart,
                formDescription,
                noun.noun,
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    combo.supplementNoun?.asPair(),
                    noun.asPair(),
                    verb.asPair(),
                )
            )
        }
    }

    fun genKomektes(): GetTasks {
        val combos = listOf(
            KomektesCombo(makeNounList("пышақ", "нож"), makeVerbListWithTranslation("кесу", "резать"), SupplementNoun("нан", "хлеб", Septik.Tabys)),
            KomektesCombo(makeNounList("балта", "топор"), makeVerbListWithTranslation("жару", "колоть"), SupplementNoun("отын", "дрова", Septik.Tabys)),
            KomektesCombo(makeNounList("машина", "автомобиль", "ат", "лошадь"), makeVerbListWithTranslation("келу", "приезжать", "бару", "ехать"), SupplementNoun("қала", "город", Septik.Barys)),
            KomektesCombo(makeNounList("қасық", "ложка", "шанышқы", "вилка"), makeVerbListWithTranslation("жеу", "кушать"), SupplementNoun("ботқа", "каша", Septik.Tabys)),
            KomektesCombo(makeNounList("қалам", "ручка", "қарындаш", "карандаш", "бор", "мел"), makeVerbListWithTranslation("жазу", "писать")),
            KomektesCombo(makeNounList("сабын", "мыло"), makeVerbListWithTranslation("жуу", "мыть"), SupplementNoun("қол", "рука", Septik.Tabys)),
            KomektesCombo(makeNounList("сүрткіш", "тряпка"), makeVerbListWithTranslation("сүрту", "вытирать"), SupplementNoun("тақта", "доска", Septik.Tabys)),
            KomektesCombo(makeNounList("кесе", "пиала"), makeVerbListWithTranslation("ішу", "пить"), SupplementNoun("шай", "чай", Septik.Tabys)),
            KomektesCombo(makeNounList("жолдас", "товарищ"), makeVerbListWithTranslation("бару", "идти"), SupplementNoun("театр", "театр", Septik.Barys)),
            KomektesCombo(makeNounList("дәрігер", "врач"), makeVerbListWithTranslation("ақылдасу", "советоваться")),
        )

        return collectTasks { taskId ->
            val grammarForm = usedForms.random()
            val possForm = usedForms.random()
            val combo = combos.random()
            val noun = combo.tools.random()
            val verb = combo.verbs.random()
            val verbBuilder = verb.builder()
            val sentenceType = SentenceType.Statement

            val supplementNoun = combo.supplementNoun
            val supplement = if (supplementNoun != null) {
                val supForm = supplementNoun.form(grammarForm)
                if (supForm != null) {
                    "${supForm} "
                } else {
                    LOG.info("genKomektes: bad supplement form for ${supplementNoun} with grammarForm ${grammarForm}")
                    ""
                }
            } else {
                ""
            }

            val nounForm = noun.builder().possessiveSeptikForm(possForm.person, possForm.number, Septik.Komektes)

            val verbForm = if (Random.nextBoolean()) {
                verbBuilder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType).raw
            } else {
                verbBuilder.past(grammarForm.person, grammarForm.number, sentenceType).raw
            }

            val sentenceStart = "${grammarForm.pronoun} ${supplement}"
            val formDescriptionSb = StringBuilder("творительный падеж для ")
            val formHintSb = StringBuilder(noun.noun)
            formDescriptionSb.append(possForm.ruShort)
            formHintSb.append(" ∈ ${possForm.pronoun}")

            val description = buildSeptikDescription(
                sentenceStart,
                formDescriptionSb.toString(),
                formHintSb.toString(),
                verbForm,
            )

            TaskItem(
                description,
                buildAnswers(sentenceStart, " ${verbForm}", nounForm),
                translations = collectTranslations(
                    supplementNoun?.asPair(),
                    noun.asPair(),
                    verb.asPair(),
                )
            )
        }
    }

    private fun generateTasks(topic: TaskTopic): GetTasks? {
        return when (topic) {
            TaskTopic.CONJ_PRESENT_TRANSITIVE_EASY -> genPresentTransitiveEasy()
            TaskTopic.CONJ_PRESENT_TRANSITIVE -> genPresentTransitive()
            TaskTopic.CONJ_PRESENT_CONTINUOUS_EASY -> genPresentContinuousEasy()
            TaskTopic.CONJ_PRESENT_CONTINUOUS -> genPresentContinuous()
            TaskTopic.CONJ_PAST_EASY -> genPastEasy()
            TaskTopic.CONJ_PAST -> genPast()
            TaskTopic.CONJ_REMOTE_PAST_EASY -> genRemotePastEasy()
            TaskTopic.CONJ_REMOTE_PAST -> genRemotePast()
            TaskTopic.CONJ_PAST_UNCERTAIN_EASY -> genPastUncertainEasy()
            TaskTopic.CONJ_PAST_UNCERTAIN -> genPastUncertain()
            TaskTopic.CONJ_OPTATIVE_MOOD_EASY -> genOptativeMoodEasy()
            TaskTopic.CONJ_OPTATIVE_MOOD -> genOptativeMood()
            TaskTopic.CONJ_OPTATIVE_MOOD_PAST -> genOptativeMoodPastTense()
            TaskTopic.CONJ_CONDITIONAL_MOOD -> genConditionalMood()
            TaskTopic.CONJ_CAN_CLAUSE_EASY -> genCanClauseEasy()
            TaskTopic.CONJ_CAN_CLAUSE -> genCanClause()
            TaskTopic.CONJ_CAN_CLAUSE_PAST -> genCanClausePastTense()
            TaskTopic.CONJ_UNAU_CLAUSE -> genUnauClause()
            TaskTopic.CONJ_UNATU_CLAUSE -> genUnatuClause()
            TaskTopic.CONJ_KORU_CLAUSE -> genKoruClause()
            TaskTopic.CONJ_TRY_CLAUSE_EASY -> genTryClauseEasy()
            TaskTopic.CONJ_TRY_CLAUSE -> genTryClause()
            TaskTopic.CONJ_JAZDAU_CLAUSE -> genJazdauClause()
            TaskTopic.CONJ_CONJUNCTIVE_AR -> genConjunctiveAr()
            TaskTopic.DECL_TABYS_EASY -> genTabysEasy()
            TaskTopic.DECL_TABYS -> genTabys()
            TaskTopic.DECL_ILIK_EASY -> genIlikEasy()
            TaskTopic.DECL_ILIK -> genIlik()
            TaskTopic.DECL_BARYS_EASY -> genBarysEasy()
            TaskTopic.DECL_BARYS -> genBarys()
            TaskTopic.DECL_JATYS_EASY -> genJatysEasy()
            TaskTopic.DECL_JATYS -> genJatys()
            TaskTopic.DECL_SHYGYS_EASY -> genShygysEasy()
            TaskTopic.DECL_SHYGYS -> genShygys()
            TaskTopic.DECL_KOMEKTES_EASY -> genKomektesEasy()
            TaskTopic.DECL_KOMEKTES -> genKomektes()
            else -> null
        }
    }

    fun generateTopicTasks(topic: TaskTopic): GetTasks? {
        val tasks = generateTasks(topic) ?: return null
        return tasks.copy(
            references = TheoryReferenceProvider.provide(topic)
        )
    }
}