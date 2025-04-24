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

    private fun buildTaskDescription(sentenceStart: String, verb: String, forceExceptional: Boolean): String {
        val sb = StringBuilder()
        sb.append("(переходное время)\n")
        val noteOnException = if (forceExceptional) {
            ", глагол-исключение"
        } else {
            ""
        }
        sb.append("`${sentenceStart}[${verb}${noteOnException}]`\n")
        return sb.toString()
    }

    private fun genPresentTransitiveEasy(): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..kTaskCount) {
            val verb = Top100.pickRandom()
            val builder = VerbBuilder(verb.verbDictForm, verb.forceExceptional)
            val grammarForm = usedForms.random()
            val phrasal = builder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)

            val sentenceStart = buildSentenceStart(grammarForm.pronoun, verb.randomPreceding())

            val description = buildTaskDescription(
                sentenceStart,
                verb.verbDictForm,
                verb.forceExceptional
            )

            tasks.add(TaskItem(
                description,
                listOf("${sentenceStart}${phrasal.raw}")
            ))
        }
        return GetTasks(tasks)
    }

    fun generateTopicTasks(topic: TaskTopic): GetTasks? {
        return when (topic) {
            TaskTopic.CONJ_PRESENT_TRANSITIVE_EASY -> genPresentTransitiveEasy()
            else -> null
        }
    }
}