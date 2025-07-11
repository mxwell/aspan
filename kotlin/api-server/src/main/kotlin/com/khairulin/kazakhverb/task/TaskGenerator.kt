package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.*
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem
import io.ktor.util.logging.*
import kotlin.random.Random

typealias TConjFormBuilder = (verb: String, sentenceType: SentenceType) -> String

class TaskGenerator {
    private val LOG = KtorSimpleLogger("TaskGenerator")

    companion object {
        fun collectTranslationsOfList(pairs: List<Pair<String, String>?>): List<List<String>> {
            return pairs.filterNotNull().filter{ it.second.isNotEmpty() }.map {
                listOf(it.first, it.second)
            }
        }

        private val usedForms = GrammarForm.kMainForms
        private val pluralForms = GrammarForm.entries.filter { it.number == GrammarNumber.Plural }
    }

    private val kTaskCount = 10

    private val particleGenerator: ParticipleTaskGenerator by lazy {
        ParticipleTaskGenerator(kTaskCount)
    }

    private fun pickFormExcept(except: List<GrammarForm>, defaultForm: GrammarForm): GrammarForm {
        for (i in 0..9) {
            val form = usedForms.random()
            if (!except.contains(form)) {
                return form
            }
        }
        return defaultForm
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
        val label = buildConjugationHint(tense, sentenceType)
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
            " + ${auxVerb}"
        } else {
            ""
        }
        val questionMark = if (sentenceType == SentenceType.Question) {
            "?"
        } else {
            ""
        }
        val pattern = "${sentenceStart}${subjectPart}[${verb}${noteOnException}${auxVerbPart}]${questionMark}"
        return TaskDescription.compose(label, pattern)
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
            for (attempt in 1..3) {
                val task = generator(taskId)
                if (attempt == 3 || tasks.isEmpty() || tasks.last().question != task.question) {
                    tasks.add(task)
                    break
                }
            }
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
            AuxVerbProvider.jatuBuilder,
        )
        answers.add("${sentenceStart}${phrasal.raw}")
        if (sentenceType == SentenceType.Negative) {
            val phrasal2 = builder.presentContinuousForm(
                grammarForm.person,
                grammarForm.number,
                sentenceType,
                AuxVerbProvider.jatuBuilder,
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

    data class NotHappeningCombo(
        val subject: NounInfo?,
        val middle: String,
        val verb: VerbInfo,
        val forms: List<GrammarForm> = emptyList(),
        val aux: VerbBuilder = AuxVerbProvider.jatuBuilder,
        val translations: List<Pair<String, String>?> = emptyList(),
    ) {
        fun getGrammarForms() = if (forms.isNotEmpty()) {
            forms
        } else {
            usedForms
        }
    }

    /**
     * Радио сөйлемей тұр
     * Жаңбыр жаумай тұр
     * Машина жүрмей жатыр
     * мен ұйықтай алмай жатырмын
     * Ахмет үйiне қайтпай жүр
     * компьютер істемей жатыр
     * көңіл-күйім болмай тұр
     * Марат дүкенге бармай жатыр
     * біз кездесе алмай жатырмыз
     * жаңбыр бітпей жатыр
     * менің хатым келмей жатыр
     * Ол ештеңе айтпай тұр
     * Сабақ басталмай тұр
     * Компьютер қосылмай жатыр
     * Мен оны түсінбей тұрмын
     * Кітапхана ашылмай жатыр.
     */
    private val kNotHappeningCombos = listOf(
        NotHappeningCombo(
            NounInfo("радио", "радио"),
            " ",
            VerbInfo("сөйлеу", translation = "говорить"),
            aux = AuxVerbProvider.turuBuilder,
        ),
        NotHappeningCombo(
            NounInfo("жаңбыр", "дождь"),
            " ",
            VerbInfo("жауу", translation = "идти"),
            aux = AuxVerbProvider.turuBuilder,
        ),
        NotHappeningCombo(
            NounInfo("машина", translation = "автомобиль"),
            " ",
            VerbInfo("жүру", translation = "ездить"),
        ),
        NotHappeningCombo(
            null,
            " ",
            VerbInfo("ұйықтау", translation = "спать")
        ),
        NotHappeningCombo(
            NounInfo("Ахмет", translation = ""),
            " үйiне ",
            VerbInfo("қайту", translation = "возвращаться"),
            aux = AuxVerbProvider.juruBuilder,
            translations = listOf(Pair("үй", "дом")),
        ),
        NotHappeningCombo(
            NounInfo("компьютер", translation = "компьютер"),
            " ",
            VerbInfo("істеу", translation = "работать"),
        ),
        NotHappeningCombo(
            NounInfo("көңіл-күйім", translation = "моё настроение"),
            " ",
            VerbInfo("болу", translation = "быть"),
            aux = AuxVerbProvider.turuBuilder,
        ),
        NotHappeningCombo(
            NounInfo("Марат", translation = ""),
            " дүкенге ",
            VerbInfo("бару", translation = "идти"),
            translations = listOf(Pair("дүкен", "магазин")),
        ),
        NotHappeningCombo(
            null,
            " ",
            VerbInfo("кездесу", translation = "встречаться"),
            forms = pluralForms,
        ),
        NotHappeningCombo(
            NounInfo("жаңбыр", "дождь"),
            " ",
            VerbInfo("біту", translation = "заканчиваться"),
        ),
        NotHappeningCombo(
            NounInfo("менің хатым", translation = "моё письмо"),
            " ",
            VerbInfo("келу", translation = "приходить"),
        ),
        NotHappeningCombo(
            null,
            " ештеңе ",
            VerbInfo("айту", translation = "говорить"),
            aux = AuxVerbProvider.turuBuilder,
            translations = listOf(Pair("ештеңе", "ничего")),
        ),
        NotHappeningCombo(
            NounInfo("сабақ", "урок"),
            " ",
            VerbInfo("басталу", translation = "начинаться"),
            aux = AuxVerbProvider.turuBuilder,
        ),
        NotHappeningCombo(
            null,
            " оны ",
            VerbInfo("түсіну", translation = "понимать"),
            aux = AuxVerbProvider.turuBuilder,
        ),
        NotHappeningCombo(
            NounInfo("кітапхана", "библиотека"),
            " ",
            VerbInfo("ашылу", translation = "открываться"),
        ),
    )

    private fun genNotHappening() = collectTasks {
        val combo = kNotHappeningCombos.random()
        val grammarForm = if (combo.subject != null) {
            GrammarForm.OL
        } else {
            combo.getGrammarForms().random()
        }
        val subject = if (combo.subject != null) {
            combo.subject.noun
        } else {
            grammarForm.pronoun
        }
        val sentenceStart = "${subject}${combo.middle}"
        val auxVerb = combo.aux.verbDictForm
        val alu = grammarForm.person == GrammarPerson.First
        val aluHint = if (alu) {
            "+ алу "
        } else {
            ""
        }
        val label = "что-то никак не происходит"
        val pattern = "${sentenceStart}[${combo.verb.verb} ${aluHint}+ ${auxVerb}]"
        val builder = combo.verb.builder()
        val verbForm = if (alu) {
            builder.canClauseInPresentContinuous(grammarForm.person, grammarForm.number, SentenceType.Negative, combo.aux).raw
        } else {
            builder.presentContinuousForm(grammarForm.person, grammarForm.number, SentenceType.Negative, combo.aux, negateAux = false).raw
        }
        val answer = "${sentenceStart}${verbForm}"
        TaskItem(
            TaskDescription.compose(label, pattern),
            listOf(
                answer
            ),
            translations = (
                collectTranslations(combo.subject?.asPair())
                + collectTranslationsOfList(combo.translations)
                + collectTranslations(combo.verb.asPair())
            ),
        )
    }

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
        val (first, second) = combo.affinity.getRandomGrammarFormPair()

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

        val labelSb = StringBuilder()
        labelSb.append("условное наклонение")
        if (combo.firstSentence == SentenceType.Negative) {
            labelSb.append(", *отрицание*")
        }
        labelSb.append(", ${first.ruShort}")
        labelSb.append(" + ")
        labelSb.append("переходное время")
        if (combo.secondSentence == SentenceType.Negative) {
            labelSb.append(", *отрицание*")
        }
        labelSb.append(", ${second.ruShort}")
        val label = labelSb.toString()

        val patternSb = StringBuilder()
        patternSb.append(firstStart)
        patternSb.append("[${combo.firstVerb.verb}]")
        patternSb.append(", ")
        patternSb.append(secondStart)
        patternSb.append("[${combo.secondVerb.verb}]")
        val pattern = patternSb.toString()

        val answer = "${firstStart}${firstVerbForm}, ${secondStart}${secondVerbForm}"

        TaskItem(
            TaskDescription.compose(label, pattern),
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
            val label = if (negation) {
                "не нравится, переходное время"
            } else {
                "нравится, переходное время"
            }
            val pronoun = grammarForm.pronoun
            val pattern = "[${pronoun}] ${subject.first} [ұнау]"
            val dative = grammarForm.dative
            val verb = if (negation) "ұнамайды" else "ұнайды"
            val answer = "${dative} ${subject.first} ${verb}"
            tasks.add(
                TaskItem(
                    TaskDescription.compose(label, pattern),
                    listOf(answer)
                )
            )
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

    private val kBastauClauseTenseVariants = listOf(
        VerbTense.TensePresentTransitive,
        VerbTense.TensePast,
    )

    private fun bastauClauseGenerator(combo: Combo): TaskItem {
        val verbEntry = combo.verb
        val verb = verbEntry.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val tense = kBastauClauseTenseVariants.random()

        val phrasal = verb.builder().bastauClauseOfTense(
            grammarForm.person,
            grammarForm.number,
            tense,
        )

        val supPair = buildSupplementForm(grammarForm, verbEntry.supplements)
        val sentenceStart = buildSentenceStart(grammarForm.getPronounByTense(tense), supPair?.second ?: "")

        val description = buildTaskDescription(
            "конструкция с бастау - начало действия, ${tense.ruName}",
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

    private fun genBastauClauseTasks(pattern: SentenceTypePattern, generator: (combo: Combo) -> TaskItem): GetTasks {
        val used = mutableSetOf<String>()
        val result = mutableListOf<Combo>()
        for (taskId in 1..kTaskCount) {
            var verbEntry = VerbList.bastauCompatEntries.random()
            var grammarForm = GrammarForm.getMainRandom()
            for (retry in 1..5) {
                val key = "${verbEntry.verb.verb} + ${grammarForm.pronoun}"
                if (used.contains(key)) {
                    verbEntry = VerbList.bastauCompatEntries.random()
                    grammarForm = GrammarForm.getMainRandom()
                    continue
                } else {
                    used.add(key)
                    break
                }
            }
            val sentenceType = pattern.getSentenceTypeByTaskId(taskId)
            result.add(Combo(taskId, verbEntry, grammarForm, sentenceType))
        }
        return collectTasks { taskId ->
            generator(result[taskId - 1])
        }
    }

    private fun genBastauClause() = genBastauClauseTasks(SentenceTypePattern.S10, this::bastauClauseGenerator)

    data class QoyuCombo(
        val verb: VerbInfo,
        val supplements: List<SupplementNoun>,
    ) {
        fun translations(): List<List<String>> {
            val result = mutableListOf<List<String>>()
            for (sup in supplements) {
                val p = sup.asPair()
                result.add(listOf(p.first, p.second))
            }
            verb.asPair()?.let {
                result.add(listOf(it.first, it.second))
            }
            return result.toList()
        }
    }

    private val kQoyuCombos = listOf(
        QoyuCombo(
            VerbInfo("шегу", translation = "курить"),
            supplements = listOf(
                SupplementNoun("темекі", "табак", Septik.Atau),
            )
        ),
        QoyuCombo(
            VerbInfo("тарту", translation = "курить"),
            supplements = listOf(
                SupplementNoun("темекі", "табак", Septik.Atau),
            )
        ),
        // Марафонға қатысқаннан кейін жүгіруді қойдым
        QoyuCombo(
            VerbInfo("жүгіру", translation = "бегать"),
            supplements = listOf(
                SupplementNoun("марафон", "марафон", Septik.Barys),
                SupplementNoun("қатысқаннан", "участвовать", null, initialForm = "қатысу"),
                SupplementNoun.notNoun("кейін", "после"),
            )
        ),
        // Ол он елге барып келгеннен кейін саяхаттауды қойды
        QoyuCombo(
            VerbInfo("саяхаттау", translation = "путешествовать"),
            supplements = listOf(
                SupplementNoun("он", "десять", null),
                SupplementNoun("ел", "страна", Septik.Barys),
                SupplementNoun("барып келгеннен", "съездить", null, initialForm = "барып келу"),
                SupplementNoun.notNoun("кейін", "после"),
            )
        ),
        // Ол ауылдан көшіп, балық аулауды қойды
        QoyuCombo(
            VerbInfo("балық аулау", translation = "рыбу ловить"),
            supplements = listOf(
                SupplementNoun("ауыл", "аул", Septik.Shygys),
                SupplementNoun("көшіп,", "переехать", null, initialForm = "көшу"),
            )
        ),
        // Олар арақ ішуді қойды
        QoyuCombo(
            VerbInfo("ішу", translation = "пить"),
            supplements = listOf(
                SupplementNoun("арақ", "водка", Septik.Atau),
            ),
        ),
        // .. үйде тамақ пісіруді қойдық
        QoyuCombo(
            VerbInfo("пісіру", translation = "готовить"),
            supplements = listOf(
                SupplementNoun("үй", "дом", Septik.Jatys),
                SupplementNoun("тамақ", "пища", Septik.Atau),
            )
        ),
        // Ол киноға баруды қойды
        QoyuCombo(
            VerbInfo("бару", translation = "ходить"),
            supplements = listOf(
                SupplementNoun("кино", "кино", Septik.Barys),
            )
        ),
        // Мен интернетте отыруды қойдым
        QoyuCombo(
            VerbInfo("отыру", translation = "сидеть"),
            supplements = listOf(
                SupplementNoun("интернет", "интернет", Septik.Jatys),
            )
        ),
        // Аружан ән айтуды қойды.
        QoyuCombo(
            VerbInfo("ән айту", translation = "петь"),
            supplements = emptyList()
        ),
        // Біз кешке дейін жұмыс істеуді қойдық
        QoyuCombo(
            VerbInfo("жұмыс істеу", translation = "работать"),
            supplements = listOf(
                SupplementNoun("кеш", "вечер", Septik.Barys),
                SupplementNoun.notNoun("дейін", "до"),
            )
        ),
        // TODO support imperative, e.g. Сен оны мазалауды қойшы!
    )

    private fun genQoyuClause() = collectTasks {
        val grammarForm = GrammarForm.getMainRandom()
        val combo = kQoyuCombos.random()
        val sentenceType = SentenceType.Statement

        val mainVerbForm = NounBuilder.ofNoun(combo.verb.verb).septikForm(Septik.Tabys).raw

        val auxVerbForm = VerbBuilder("қою").past(grammarForm.person, grammarForm.number, sentenceType).raw

        val tense = VerbTense.TensePast
        val middle = combo.supplements.map { it.form(grammarForm)!! }.joinToString(" ")
        val sentenceStart = buildSentenceStart(grammarForm.getPronounByTense(tense), middle)

        val description = buildTaskDescription(
            "конструкция с қою - прекращение действия",
            sentenceStart,
            combo.verb,
            sentenceType,
            auxVerb = "қою",
        )

        TaskItem(
            description,
            listOf(
                "${sentenceStart}${mainVerbForm} ${auxVerbForm}"
            ),
            translations = combo.translations(),
        )
    }

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
        val label = "действие чуть не совершилось"
        val pattern = "${sentenceStart} [${combo.verb.verb} + ${combo.auxVerb} + жаздау]"
        val auxBuilder = VerbBuilder(combo.auxVerb)
        val verbForm = combo
            .verb.builder()
            .jazdauClause(grammarForm.person, grammarForm.number, auxBuilder)
            .raw
        val answer = "${sentenceStart} ${verbForm}"

        TaskItem(
            TaskDescription.compose(label, pattern),
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

    private fun getConditionalForm(verb: String, grammarForm: GrammarForm, sentenceType: SentenceType = SentenceType.Statement): String {
        return VerbBuilder(verb)
            .conditionalMood(grammarForm.person, grammarForm.number, sentenceType)
            .raw
    }

    private fun getArForm(verb: String, sentenceType: SentenceType): String {
        return VerbBuilder(verb)
            .possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, sentenceType)
            .raw
    }

    private fun getAtynForm(verb: String, sentenceType: SentenceType): String {
        return VerbBuilder(verb)
            .pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, sentenceType)
            .raw
    }

    private fun getUshyForm(verb: String, sentenceType: SentenceType): String {
        return VerbBuilder(verb)
            .ushyUshiForm(sentenceType)
            .raw
    }

    private fun getEdiForm(grammarForm: GrammarForm): String {
        return VerbBuilder("еу")
            .past(grammarForm.person, grammarForm.number, SentenceType.Statement)
            .raw
    }

    private fun buildConjunctiveDescription(conjName: String, grammarForm: GrammarForm, pattern: String, sentenceType: SentenceType = SentenceType.Statement): String {
        val sentenceTypePart = when (sentenceType) {
            SentenceType.Statement -> ""
            SentenceType.Negative -> ", *отрицание*"
            else -> throw IllegalArgumentException("sentenceType ${sentenceType} not supported")
        }
        val label = "${conjName}, ${grammarForm.ruShort}${sentenceTypePart}"
        return TaskDescription.compose(label, pattern)
    }

    private fun conjunctiveArGen1(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        val (first, second) = GrammarFormAffinity.mismatchRequired.getRandomGrammarFormPair()
        val firstVerbForm = getConditionalForm("білу", first)
        val secondBarys = second.dative
        val secondVerbForm = conjFormBuilder("айту", SentenceType.Statement)
        val ediForm = getEdiForm(first)
        val sentenceStart = "${first.pronoun} осыны ${firstVerbForm}, ${first.pronoun} ${secondBarys}"
        val template = "${sentenceStart} [айту]"
        val description = buildConjunctiveDescription(conjName, first, template)
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

    private fun possForm(noun: String, form: GrammarForm, septik: Septik) = NounBuilder.ofNoun(noun)
        .possessiveSeptikForm(form.person, form.number, septik)
        .raw

    private fun conjunctiveArGen2(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // мүмкіндігІМ болса, МЕН машина сатып алар едіМ
        val grammarForm = usedForms.random()
        val firstAtau = possForm("мүмкіндік", grammarForm, Septik.Atau)
        val firstVerbForm = getConditionalForm("болу", GrammarForm.OL)
        val secondVerb = "сатып алу"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)
        val sentenceStart = "${firstAtau} ${firstVerbForm}, ${grammarForm.pronoun} машина"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template)
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

    private fun conjunctiveArGen3(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // МЕН бизнесмен болсаМ, көп ақшаМ болар еді
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("болу", grammarForm)
        val secondAtauForm = NounBuilder.ofNoun("ақша")
            .possessiveSeptikForm(grammarForm.person, grammarForm.number, Septik.Atau)
            .raw
        val secondVerb = "болу"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(GrammarForm.OL)

        val sentenceStart = "${grammarForm.pronoun} бизнесмен ${firstVerbForm}, көп ${secondAtauForm}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, GrammarForm.OL, template)
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

    private fun conjunctiveArGen4(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // егер МЕНДЕ көп ақша болса, МЕН үй сатып алар едіМ
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("болу", GrammarForm.OL)
        val secondVerb = "сатып алу"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.jatys} көп ақша ${firstVerbForm}, ${grammarForm.pronoun} үй"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, GrammarForm.OL, template)
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

    private fun conjunctiveArGen5(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // егер СЕН ертерек тұрсаҢ мектепке кешікпес едіҢ
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("тұру", grammarForm)
        val secondVerb = "кешігу"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Negative)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} ертерек ${firstVerbForm}, мектепке"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template, sentenceType = SentenceType.Negative)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("ертерек", "пораньше"),
                listOf("тұру", "вставать"),
                listOf("мектеп", "школа"),
                listOf("кешігу", "опаздывать"),
            )
        )
    }

    private fun conjunctiveArGen6(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // егер ОЛ шетелге барсА, көп нәрсе үйренер едІ
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("бару", grammarForm)
        val secondVerb = "үйрену"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} шетелге ${firstVerbForm}, көп нәрсе"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("шетел", "заграница"),
                listOf("бару", "ехать"),
                listOf("көп", "много"),
                listOf("нәрсе", "вещь"),
                listOf("үйрену", "научиться"),
            )
        )
    }

    private fun conjunctiveArGen7(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // егер СЕН келсеҢ, біз бірге ойнар едіК
        val firstForm = pickFormExcept(listOf(GrammarForm.BIZ), GrammarForm.SEN)
        val secondForm = GrammarForm.BIZ
        val firstVerbForm = getConditionalForm("келу", firstForm)
        val secondVerb = "ойнау"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(secondForm)

        val sentenceStart = "егер ${firstForm.pronoun} ${firstVerbForm}, ${secondForm.pronoun} бірге"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, secondForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("келу", "приходить"),
                listOf("бірге", "вместе"),
                listOf("ойнау", "играть"),
            )
        )
    }

    private fun conjunctiveArGen8(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // егер ауа райы жақсы болса, БІЗ саябаққа барар едіК
        val secondForm = usedForms.random()
        val firstVerbForm = getConditionalForm("болу", GrammarForm.OL)
        val secondVerb = "бару"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(secondForm)

        val sentenceStart = "егер ауа райы жақсы ${firstVerbForm}, ${secondForm.pronoun} саябаққа"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, secondForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("ауа райы", "погода"),
                listOf("жақсы", "хороший"),
                listOf("болу", "быть"),
                listOf("саябақ", "парк"),
                listOf("бару", "идти"),
            )
        )
    }

    private fun conjunctiveArGen9(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер МЕН бай болсаМ, көп қайырымдылық жасар едіМ
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("болу", grammarForm)
        val secondVerb = "жасау"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} бай ${firstVerbForm}, көп қайырымдылық"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("бай", "богатый"),
                listOf("болу", "быть"),
                listOf("көп", "много"),
                listOf("қайырымдылық", "благотворительность"),
                listOf("жасау", "делать"),
            )
        )
    }

    private fun conjunctiveArGen10(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер БІЗ ертерек шықсаҚ, пойызға үлгерер едіК.
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("шығу", grammarForm)
        val secondVerb = "үлгеру"
        val secondVerbForm = conjFormBuilder(secondVerb, SentenceType.Statement)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} ертерек ${firstVerbForm}, пойызға"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("ертерек", "пораньше"),
                listOf("шығу", "выходить"),
                listOf("пойыз", "поезд"),
                listOf("үлгеру", "успевать"),
            )
        )
    }

    private fun conjunctiveArGen11(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер ОЛ келмесе, БІЗ бастамас едІК.
        val (first, second) = GrammarFormAffinity.mismatchRequired.getRandomGrammarFormPair()
        val firstVerbForm = getConditionalForm("келу", first, sentenceType = SentenceType.Negative)
        val secondVerb = "бастау"
        val secondSentenceType = SentenceType.Negative
        val secondVerbForm = conjFormBuilder(secondVerb, secondSentenceType)
        val ediForm = getEdiForm(second)

        val sentenceStart = "егер ${first.pronoun} ${firstVerbForm}, ${second.pronoun}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, second, template, sentenceType = secondSentenceType)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("келу", "приходить"),
                listOf("бастау", "начинать"),
            )
        )
    }

    private fun conjunctiveArGen12(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер біз асықпасақ, автобусқа үлгермес едік.
        val grammarForm = usedForms.random()
        val firstVerbForm = getConditionalForm("асығу", grammarForm, sentenceType = SentenceType.Negative)
        val secondVerb = "үлгеру"
        val secondSentenceType = SentenceType.Negative
        val secondVerbForm = conjFormBuilder(secondVerb, secondSentenceType)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} ${firstVerbForm}, автобусқа"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template, sentenceType = secondSentenceType)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("асығу", "спешить"),
                listOf("автобус", "автобус"),
                listOf("үлгеру", "успевать"),
            )
        )
    }

    private fun conjunctiveArGen13(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер ол қатесін түсінбесе, ол өзгермес еді.
        val grammarForm = usedForms.random()
        val tabysForm = possForm("қате", grammarForm, Septik.Tabys)
        val firstVerbForm = getConditionalForm("түсіну", grammarForm, sentenceType = SentenceType.Negative)
        val secondVerb = "өзгеру"
        val secondSentenceType = SentenceType.Negative
        val secondVerbForm = conjFormBuilder(secondVerb, secondSentenceType)
        val ediForm = getEdiForm(grammarForm)

        val sentenceStart = "егер ${grammarForm.pronoun} ${tabysForm} ${firstVerbForm}, ${grammarForm.pronoun}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, grammarForm, template, sentenceType = secondSentenceType)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("қате", "ошибка"),
                listOf("түсіну", "понимать"),
                listOf("өзгеру", "изменяться"),
            )
        )
    }

    private fun conjunctiveArGen14(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер олар айтпаса, біз білмес едік.
        val (first, second) = GrammarFormAffinity.mismatchRequired.getRandomGrammarFormPair()
        val firstVerbForm = getConditionalForm("айту", first, sentenceType = SentenceType.Negative)
        val secondVerb = "білу"
        val secondSentenceType = SentenceType.Negative
        val secondVerbForm = conjFormBuilder(secondVerb, secondSentenceType)
        val ediForm = getEdiForm(second)

        val sentenceStart = "егер ${first.pronoun} ${firstVerbForm}, ${second.pronoun}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, second, template, sentenceType = secondSentenceType)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("айту", "сказать"),
                listOf("білу", "знать"),
            )
        )
    }

    private fun conjunctiveArGen15(conjName: String, conjFormBuilder: TConjFormBuilder): TaskItem {
        // Егер сен ескертпесең, мен байқамас едім.
        val (first, second) = GrammarFormAffinity.mismatchRequired.getRandomGrammarFormPair()
        val firstVerbForm = getConditionalForm("ескерту", first, sentenceType = SentenceType.Negative)
        val secondVerb = "байқау"
        val secondSentenceType = SentenceType.Negative
        val secondVerbForm = conjFormBuilder(secondVerb, secondSentenceType)
        val ediForm = getEdiForm(second)

        val sentenceStart = "егер ${first.pronoun} ${firstVerbForm}, ${second.pronoun}"
        val template = "${sentenceStart} [${secondVerb}]"
        val description = buildConjunctiveDescription(conjName, second, template, sentenceType = secondSentenceType)
        val answer = "${sentenceStart} ${secondVerbForm} ${ediForm}"
        return TaskItem(
            description,
            listOf(answer),
            translations = listOf(
                listOf("егер", "если"),
                listOf("ескерту", "предупреждать"),
                listOf("байқау", "замечать"),
            )
        )
    }

    private val kConjunctiveImplementations = listOf(
        this::conjunctiveArGen1,
        this::conjunctiveArGen2,
        this::conjunctiveArGen3,
        this::conjunctiveArGen4,
        this::conjunctiveArGen5,
        this::conjunctiveArGen6,
        this::conjunctiveArGen7,
        this::conjunctiveArGen8,
        this::conjunctiveArGen9,
        this::conjunctiveArGen10,
        this::conjunctiveArGen11,
        this::conjunctiveArGen12,
        this::conjunctiveArGen13,
        this::conjunctiveArGen14,
        this::conjunctiveArGen15,
    )

    private fun genConjunctiveCommon(conjName: String, conjFormBuilder: TConjFormBuilder) = collectTasks {
        kConjunctiveImplementations.random()(conjName, conjFormBuilder)
    }

    private fun genConjunctiveAr() = genConjunctiveCommon(
        "сослагательное наклонение с -ар и еді",
        this::getArForm,
    )

    private fun genConjunctiveAtyn() = genConjunctiveCommon(
        "сослагательное наклонение с -атын и еді",
        this::getAtynForm,
    )

    private fun genConjunctiveUshy() = genConjunctiveCommon(
        "сослагательное наклонение с -ушы и еді",
        this::getUshyForm,
    )

    private fun buildSeptikDescription(sentenceStart: String, label: String, objectWord: String, verbForm: String): String {
        val pattern = "${sentenceStart}[${objectWord}] ${verbForm}"
        return TaskDescription.compose(label, pattern)
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
                objectHintSb.append(" ∈ ${possForm.poss}")
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
                formHintSb.append(" ∈ ${ownerPossForm.poss}")
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
            if (translation.isNotEmpty()) {
                result.add(NounInfo(noun, translation))
            }
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
            formHintSb.append(" ∈ ${possForm.poss}")

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
            val formHint = "${place.first} ∈ ${possForm.poss}"

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
            val formHint = "${noun} ∈ ${possForm.poss}"

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
            formHintSb.append(" ∈ ${possForm.poss}")

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

    fun genPresentParticiple(): GetTasks {
        throw NotImplementedError()
    }

    data class AdjTranslated(
        val adj: String,
        val translation: String,
    ) {
        fun asList() = listOf(adj, translation)
    }

    data class AdjComparativeCombo(
        val adjs: List<AdjTranslated>,
        val head: String,
        val tail: String,
        val translations: List<List<String>>
    )

    private val kAdjComparativeCombos = listOf(
        AdjComparativeCombo(
            listOf(
                AdjTranslated("таза", "чистый"),
                AdjTranslated("үлкен", "большой"),
                AdjTranslated("жылы", "тёплый"),
            ),
            "бұл бөлме ана бөлмеден ",
            "",
            listOf(
                listOf("бөлме", "комната"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("қызық", "интересный"),
                AdjTranslated("жақсы", "хороший"),
            ),
            "бұл кітап маған ",
            " көрінді",
            listOf(
                listOf("кітап", "книга"),
                listOf("көріну", "показаться"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жұмсақ", "мягкий"),
                AdjTranslated("жақсы", "хороший"),
                AdjTranslated("әдемі", "красивый"),
            ),
            "ана диваннан мына диван ",
            "",
            listOf(
                listOf("диван", "диван"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("кіші", "младший"),
                AdjTranslated("үлкен", "старший"),
            ),
            "сенің жасың менің жасымнан ",
            "",
            listOf(
                listOf("жас", "возраст"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жылы", "тёплый"),
                AdjTranslated("ыстық", "жаркий"),
                AdjTranslated("суық", "холодный"),
                AdjTranslated("салқын", "прохладный"),
            ),
            "бүгін күн ",
            "",
            listOf(
                listOf("бүгін", "сегодня"),
                listOf("күн", "день"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жұқа", "тонкий"),
                AdjTranslated("қалың", "толстый"),
            ),
            "мына дәптер ана дәптерден ",
            "",
            listOf(
                listOf("дәптер", "тетрадь"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("қызық", "интересный"),
            ),
            "мына жазушының әңгімесі ",
            "",
            listOf(
                listOf("жазушы", "писатель"),
                listOf("әңгіме", "рассказ"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("мықты", "сильный"),
                AdjTranslated("үлкен", "большой"),
                AdjTranslated("биік", "высокий"),
            ),
            "мына палуан ",
            "",
            listOf(
                listOf("палуан", "борец"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("дәмді", "вкусный"),
            ),
            "Сіздің сорпаңыз ",
            "",
            listOf(
                listOf("сорпа", "бульон"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жіңішке", "тонкий"),
                AdjTranslated("ұзын", "длинный"),
            ),
            "",
            " жіп бар ма?",
            listOf(
                listOf("жіп", "нитка"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("арзан", "дешёвый"),
                AdjTranslated("қымбат", "дорогой"),
            ),
            "мына заттар ",
            "",
            listOf(
                listOf("зат", "вещь"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("әлсіз", "слабый"),
            ),
            "мына студенттің денсаулығы ",
            "",
            listOf(
                listOf("студент", "студент"),
                listOf("денсаулық", "здоровье"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("терең", "глубокий"),
            ),
            "",
            " қазу керек",
            listOf(
                listOf("қазу", "копать"),
                listOf("керек", "нужно"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жеңіл", "лёгкий"),
            ),
            "менің ",
            " аяқ киім алуым керек",
            listOf(
                listOf("аяқ киім", "обувь"),
                listOf("алу", "покупать"),
                listOf("керек", "нужно"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("жылы", "тёплый"),
                AdjTranslated("суық", "холодный"),
            ),
            "",
            " су ішкім келіп тұр",
            listOf(
                listOf("су", "вода"),
                listOf("ішу", "пить"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("ұзақ", "долго"),
            ),
            "",
            " күту керек болады",
            listOf(
                listOf("күту", "ждать"),
                listOf("керек", "нужно"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("биік", "высоко"),
            ),
            "мына затты ",
            " қойшы",
            listOf(
                listOf("зат", "вещь"),
                listOf("қою", "ставить"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("тәтті", "сладкий"),
            ),
            "мына бал ",
            "",
            listOf(
                listOf("бал", "мёд"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("тәтті", "сладкий"),
                AdjTranslated("қызыл", "красный"),
            ),
            "кешегі қарбыз ",
            " болды",
            listOf(
                listOf("кешегі", "вчерашний"),
                listOf("қарбыз", "арбуз"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("төмен", "низкий"),
            ),
            "олардың деңгейі ",
            "",
            listOf(
                listOf("деңгей", "уровень"),
            )
        ),
        AdjComparativeCombo(
            listOf(
                AdjTranslated("ауыр", "тяжёлый"),
            ),
            "",
            "  сөмкені маған бер",
            listOf(
                listOf("сөмке", "сумка"),
                listOf("беру", "давать"),
            )
        ),
    )

    private fun genCommonAdjComparative(label: String, formGenerator: (String) -> String) = collectTasks {
        val combo = kAdjComparativeCombos.random()
        val adj = combo.adjs.random()

        val adjForm = formGenerator(adj.adj)

        val pattern = "${combo.head}[${adj.adj}]${combo.tail}"
        val answer = "${combo.head}${adjForm}${combo.tail}"
        val translations = combo.translations.toMutableList()
        translations.add(adj.asList())

        TaskItem(
            TaskDescription.compose(label, pattern),
            listOf(answer),
            translations = translations,
        )
    }

    fun genAdjComparative() = genCommonAdjComparative("сравнительная степень с -рақ") {
        AdjBuilder(it).rakForm().raw
    }

    fun genAdjComparativeDau() = genCommonAdjComparative("сравнительная степень с -дау") {
        AdjBuilder(it).dauForm().raw
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
            TaskTopic.CONJ_BASTAU_CLAUSE -> genBastauClause()
            TaskTopic.CONJ_QOYU_CLAUSE -> genQoyuClause()
            TaskTopic.CONJ_JAZDAU_CLAUSE -> genJazdauClause()
            TaskTopic.CONJ_CONJUNCTIVE_AR -> genConjunctiveAr()
            TaskTopic.CONJ_CONJUNCTIVE_ATYN -> genConjunctiveAtyn()
            TaskTopic.CONJ_CONJUNCTIVE_USHY -> genConjunctiveUshy()
            TaskTopic.CONJ_NOT_HAPPENING -> genNotHappening()
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
            TaskTopic.ADJ_COMPARATIVE -> genAdjComparative()
            TaskTopic.ADJ_COMPARATIVE_DAU -> genAdjComparativeDau()
            TaskTopic.PARTICIPLE_PRESENT -> particleGenerator.genPresentParticiple()
            TaskTopic.PARTICIPLE_PAST -> particleGenerator.genPastParticiple()
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