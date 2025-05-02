package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.SentenceType
import com.khairulin.kazakhverb.grammar.VerbBuilder
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem
import kotlin.random.Random

class TaskGenerator {

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

    data class Combo(
        val taskId: Int,
        val verb: VerbEntry,
        val grammarForm: GrammarForm,
        val sentenceType: SentenceType,
    )

    private fun generateCombos(easy: Boolean, taskCount: Int): List<Combo> {
        val used = mutableSetOf<String>()
        val result = mutableListOf<Combo>()
        for (taskId in 1..taskCount) {
            var verb = Top100.pickRandom()
            var grammarForm = usedForms.random()
            for (retry in 1..5) {
                val key = "${verb.verbDictForm} + ${grammarForm.pronoun}"
                if (used.contains(key)) {
                    verb = Top100.pickRandom()
                    grammarForm = usedForms.random()
                    continue
                } else {
                    used.add(key)
                    break
                }
            }
            val sentenceType = if (easy) {
                SentenceType.Statement
            } else {
                getSentenceTypeByTaskId(taskId)
            }
            result.add(Combo(taskId, verb, grammarForm, sentenceType))
        }
        return result
    }

    private fun genCommon(easy: Boolean, generator: (combo: Combo) -> TaskItem): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        val combos = generateCombos(easy, kTaskCount)
        for (combo in combos) {
            tasks.add(generator(combo))
        }
        return GetTasks(tasks)
    }

    private fun genEasy(generator: (combo: Combo) -> TaskItem) = genCommon(true, generator)

    private fun genRegular(generator: (combo: Combo) -> TaskItem) = genCommon(false, generator)

    private fun getSentenceTypeByTaskId(taskId: Int): SentenceType {
        return if (taskId <= 6) {
            SentenceType.Statement
        } else if (taskId <= 8) {
            SentenceType.Negative
        } else {
            SentenceType.Question
        }
    }

    private fun presentTransitiveGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "переходное время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genPresentTransitiveEasy() = genEasy(this::presentTransitiveGenerator)

    private fun genPresentTransitive() = genRegular(this::presentTransitiveGenerator)

    private fun presentContinuousGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

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
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
            auxVerb = "жату",
        )
        return TaskItem(
            description,
            answers
        )
    }

    private fun genPresentContinuousEasy() = genEasy(this::presentContinuousGenerator)

    private fun genPresentContinuous() = genRegular(this::presentContinuousGenerator)

    private fun pastGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.past(grammarForm.person, grammarForm.number, sentenceType)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "прошедшее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genPastEasy() = genEasy(this::pastGenerator)

    private fun genPast() = genRegular(this::pastGenerator)

    private fun remotePastGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

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
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            answers
        )
    }

    private fun genRemotePastEasy() = genEasy(this::remotePastGenerator)

    private fun genRemotePast() = genRegular(this::remotePastGenerator)

    private fun optativeMoodGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.optativeMood(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val sentenceStart = buildSentenceStart(grammarForm.poss, verb.randomPreceding())

        val description = buildTaskDescription(
            "желательное наклонение",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun optativeMoodPastTenseGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.optativeMoodInPastTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val sentenceStart = buildSentenceStart(grammarForm.poss, verb.randomPreceding())

        val description = buildTaskDescription(
            "желательное наклонение, прошедшее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genOptativeMoodEasy() = genEasy(this::optativeMoodGenerator)

    private fun genOptativeMood() = genRegular(this::optativeMoodGenerator)

    private fun genOptativeMoodPastTense() = genEasy(this::optativeMoodPastTenseGenerator)

    private fun canClauseGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.canClause(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "конструкция с алу = мочь",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun canClausePastGenerator(combo: Combo): TaskItem {
        val verb = combo.verb
        val grammarForm = combo.grammarForm
        val sentenceType = combo.sentenceType
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)

        val phrasal = builder.canClauseInPastTense(
            grammarForm.person,
            grammarForm.number,
            sentenceType,
        )

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "конструкция с алу = мочь, прошедшее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        return TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genCanClauseEasy() = genEasy(this::canClauseGenerator)

    private fun genCanClause() = genRegular(this::canClauseGenerator)

    private fun genCanClausePastTense() = genEasy(this::canClausePastGenerator)

    private val kLikableSubjects = listOf(
        Pair("кітап", "кітапты"),
        Pair("гүлдер", "гүлдерді"),
        Pair("спортпен айналысу", "спортпен айналысуды"),
        Pair("демалысты жоспарлау", "демалысты жоспарлауды"),
        Pair("кешкі ас жасау", "кешкі ас жасауды"),
        Pair("көлік жүргізу", "көлік жүргізуді"),
        Pair("дүкен", "дүкенді"),
        Pair("автобус күту", "автобус күтуді"),
        Pair("хатты жазу", "хатты жазуды"),
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
            val sentenceType = getSentenceTypeByTaskId(taskId)
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

    fun generateTopicTasks(topic: TaskTopic): GetTasks? {
        return when (topic) {
            TaskTopic.CONJ_PRESENT_TRANSITIVE_EASY -> genPresentTransitiveEasy()
            TaskTopic.CONJ_PRESENT_TRANSITIVE -> genPresentTransitive()
            TaskTopic.CONJ_PRESENT_CONTINUOUS_EASY -> genPresentContinuousEasy()
            TaskTopic.CONJ_PRESENT_CONTINUOUS -> genPresentContinuous()
            TaskTopic.CONJ_PAST_EASY -> genPastEasy()
            TaskTopic.CONJ_PAST -> genPast()
            TaskTopic.CONJ_REMOTE_PAST_EASY -> genRemotePastEasy()
            TaskTopic.CONJ_REMOTE_PAST -> genRemotePast()
            TaskTopic.CONJ_OPTATIVE_MOOD_EASY -> genOptativeMoodEasy()
            TaskTopic.CONJ_OPTATIVE_MOOD -> genOptativeMood()
            TaskTopic.CONJ_OPTATIVE_MOOD_PAST -> genOptativeMoodPastTense()
            TaskTopic.CONJ_CAN_CLAUSE_EASY -> genCanClauseEasy()
            TaskTopic.CONJ_CAN_CLAUSE -> genCanClause()
            TaskTopic.CONJ_CAN_CLAUSE_PAST -> genCanClausePastTense()
            TaskTopic.CONJ_UNAU_CLAUSE -> genUnauClause()
            TaskTopic.CONJ_UNATU_CLAUSE -> genUnatuClause()
            else -> null
        }
    }
}