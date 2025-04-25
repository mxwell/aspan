package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.SentenceType
import com.khairulin.kazakhverb.grammar.VerbBuilder
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem

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
        auxVerb: String? = null
    ): String {
        val sb = StringBuilder()
        val hint = buildConjugationHint(tense, sentenceType)
        sb.append("(${hint})\n")
        val noteOnException = if (forceExceptional) {
            ", глагол-исключение"
        } else {
            ""
        }
        val auxVerb = if (auxVerb != null) {
            ", вспомогательный глагол ${auxVerb}"
        } else {
            ""
        }
        val questionMark = if (sentenceType == SentenceType.Question) {
            "?"
        } else {
            ""
        }
        sb.append("`${sentenceStart}[${verb}${noteOnException}${auxVerb}]${questionMark}`\n")
        return sb.toString()
    }

    private fun genCommon(generator: (taskId: Int) -> TaskItem): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..kTaskCount) {
            tasks.add(generator(taskId))
        }
        return GetTasks(tasks)
    }

    private fun getSentenceTypeByTaskId(taskId: Int): SentenceType {
        return if (taskId <= 6) {
            SentenceType.Statement
        } else if (taskId <= 8) {
            SentenceType.Negative
        } else {
            SentenceType.Question
        }
    }

    private fun genPresentTransitiveEasy() = genCommon {
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "переходное время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genPresentTransitive() = genCommon { taskId ->
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val sentenceType = getSentenceTypeByTaskId(taskId)

        val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "переходное время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genPresentContinuousEasy() = genCommon {
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val phrasal = builder.presentContinuousForm(
            grammarForm.person,
            grammarForm.number,
            SentenceType.Statement,
            jatuBuilder
        )

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "настоящее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            auxVerb = "жату",
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genPresentContinuous() = genCommon { taskId ->
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val sentenceType = getSentenceTypeByTaskId(taskId)

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
        TaskItem(
            description,
            answers
        )
    }

    fun genPastEasy() = genCommon {
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val phrasal = builder.past(
            grammarForm.person,
            grammarForm.number,
            SentenceType.Statement,
        )

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "прошедшее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    fun genPast() = genCommon { taskId ->
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val sentenceType = getSentenceTypeByTaskId(taskId)

        val phrasal = builder.past(grammarForm.person, grammarForm.number, sentenceType)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "прошедшее время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            sentenceType,
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    fun genRemotePastEasy() = genCommon {
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val phrasal = builder.remotePast(
            grammarForm.person,
            grammarForm.number,
            SentenceType.Statement,
        )

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            "давнопрошедшее очевидное время",
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
        )
        TaskItem(
            description,
            listOf("${sentenceStart}${phrasal.raw}")
        )
    }

    private fun genRemotePast() = genCommon { taskId ->
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val sentenceType = getSentenceTypeByTaskId(taskId)

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
        TaskItem(
            description,
            answers
        )
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
            else -> null
        }
    }
}