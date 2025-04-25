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

    private fun buildTaskDescription(sentenceStart: String, verb: String, forceExceptional: Boolean, sentenceType: SentenceType): String {
        val sb = StringBuilder()
        val hint = buildConjugationHint("переходное время", sentenceType)
        sb.append("(${hint})\n")
        val noteOnException = if (forceExceptional) {
            ", глагол-исключение"
        } else {
            ""
        }
        sb.append("`${sentenceStart}[${verb}${noteOnException}]`\n")
        return sb.toString()
    }

    private fun genCommon(generator: (taskId: Int) -> TaskItem): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..kTaskCount) {
            tasks.add(generator(taskId))
        }
        return GetTasks(tasks)
    }

    private fun genPresentTransitiveEasy() = genCommon {
        val verb = Top100.pickRandom()
        val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
        val grammarForm = usedForms.random()
        val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
            sentenceStart,
            verb.verbDictForm,
            verb.forceExceptional,
            SentenceType.Statement,
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
        val sentenceType = if (taskId <= 6) {
            SentenceType.Statement
        } else if (taskId <= 8) {
            SentenceType.Negative
        } else {
            SentenceType.Question
        }

        val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)

        val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

        val description = buildTaskDescription(
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

    fun generateTopicTasks(topic: TaskTopic): GetTasks? {
        return when (topic) {
            TaskTopic.CONJ_PRESENT_TRANSITIVE_EASY -> genPresentTransitiveEasy()
            TaskTopic.CONJ_PRESENT_TRANSITIVE -> genPresentTransitive()
            else -> null
        }
    }
}