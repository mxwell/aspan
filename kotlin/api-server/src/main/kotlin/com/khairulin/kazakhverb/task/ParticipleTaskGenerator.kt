package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.NounBuilder
import com.khairulin.kazakhverb.grammar.PronounDeclension
import com.khairulin.kazakhverb.grammar.SentenceType
import com.khairulin.kazakhverb.grammar.Septik
import com.khairulin.kazakhverb.grammar.VerbBuilder
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem

/**
 * Не істейтінін сен білмейсің
 * - /ол/ не істейді/ сен білмейсің
 * - Что он делает, ты не знаешь
 * Бірдене істейтіндерін мен естімедім
 * - /олар/ бірдене істейді/ мен естімедім
 * - Их, что-то делающих, я не слышал
 * Ештеңе істемейтініңді мен көріп тұрмын
 * - /сен/ ештеңе істемейсің/ мен көріп тұрмын
 * - Тебя, ничего не делающего, я вижу
 * Бұл жұмысты істейтіндерге мен көмектесемін
 * - /сендерге/ бұл жұмысты істейсіңдер/ мен көмектесемін
 * - Вам, эту работу делающим, я помогу
 * ертен не істейтінін мен білемін
 * - /ол/ ертен не істейтінін/ мен білемін
 * - Что он завтра будет делать, я знаю
 * Онда жұмыс істейтіндерді сендер білмейсіңдер
 * - /оларды/ Онда жұмыс істейді/ сендер білмейсіңдер
 * - Там работающих людей вы не знаете
 * Олардың бүгін келетіндерін білмедім
 * - /оларды/ бүгін келейді/ білмедім
 * - О них, сегодня приходящих, я не знал
 * Сіздің бүгін келмейтініңізді білмедік
 * - /Сіз/ бүгін келмейді/ білмедік
 * - О вас, сегодня не приходящих, мы не знали
 * Сенің қалаға баратыныңды білмедім
 * - /Сен/қалаға барасың/ білмедім
 * - О тебе, едущем в город, я не знал
 * Сатып алатынға тауарды беремін
 * - /Оған/ сатып алады/ тауарды беремін
 * - Покупающему товар отдам
 */
class ParticipleTaskGenerator(val taskCount: Int) {

    data class StringFragment(
        val text: String,
        val translations: List<Pair<String, String>> = emptyList(),
    ) {
        fun collectTranslations(dst: MutableList<List<String>>) {
            for ((word, translation) in translations) {
                if (translation.isNotEmpty()) {
                    dst.add(listOf(word, translation))
                }
            }
        }
    }

    companion object {
        private val jatuBuilder: VerbBuilder by lazy {
            VerbBuilder("жату")
        }
    }

    enum class Shak {
        presentTransitive,
        past,
        presentContinuous,
        ;

        fun apply(builder: VerbBuilder, grammarForm: GrammarForm, sentenceType: SentenceType): String {
            return when(this) {
                presentTransitive -> builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)
                past -> builder.past(grammarForm.person, grammarForm.number, sentenceType)
                presentContinuous -> builder.presentContinuousForm(grammarForm.person, grammarForm.number, sentenceType, jatuBuilder)
            }.raw
        }
    }

    data class Combo(
        val affinity: GrammarFormAffinity,
        val subordinateSeptik: Septik,
        val subordinateVerb: VerbInfo,
        val mainVerb: VerbInfo,
        val shak: Shak,
        val subordinatePrefix: StringFragment? = null,
        val subordinateVerbNegative: Boolean = false,
        val mainPrefix: StringFragment? = null,
        val mainVerbNegative: Boolean = false,
    ) {
        fun subordinateSentenceType() = SentenceType.ofNegativeFlag(subordinateVerbNegative)
        fun mainSentenceType() = SentenceType.ofNegativeFlag(mainVerbNegative)

        private fun stringFragmentToString(fragment: StringFragment?): String {
            return if (fragment != null) {
                "${fragment.text} "
            } else {
                ""
            }
        }

        fun subordinatePrefixString() = stringFragmentToString(subordinatePrefix)
        fun mainPrefixString() = stringFragmentToString(mainPrefix)

        fun collectTranslations(): List<List<String>> {
            val result = mutableListOf<List<String>>()
            subordinatePrefix?.collectTranslations(result)
            subordinateVerb.collectTranslations(result)
            mainPrefix?.collectTranslations(result)
            mainVerb.collectTranslations(result)
            return result.toList()
        }
    }

    private val kCombos = listOf(
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("есту", translation = "слышать"),
            Shak.past,
            subordinatePrefix = StringFragment("бірдеңе", listOf(Pair("бірдеңе", "что-то"))),
            mainVerbNegative = true,
        )
    )

    private fun buildTask(combo: Combo): TaskItem {
        val (firstForm, secondForm) = combo.affinity.getRandomGrammarFormPair()
        val subordinateSeptik = combo.subordinateSeptik
        val descriptor = "причастие наст. вр. ${firstForm.ruShort}, ${subordinateSeptik.ruShort}"
        val subordinateVerbBuilder = combo.subordinateVerb.builder()
        val hintVerbForm = subordinateVerbBuilder.presentTransitiveForm(
            firstForm.person,
            firstForm.number,
            combo.subordinateSentenceType()
        ).raw
        val pronounForm = PronounDeclension.getPronounForm(firstForm, combo.subordinateSeptik)
        val mainVerbForm = combo.shak.apply(
            combo.mainVerb.builder(),
            secondForm,
            combo.mainSentenceType(),
        )
        val mainClause = "${combo.mainPrefixString()}${mainVerbForm}"
        val subordinatePrefix = combo.subordinatePrefixString()
        val pattern = "[${firstForm.pronoun} ${subordinatePrefix}${hintVerbForm}] [${pronounForm}] ${mainClause}"

        val participleBuilder = subordinateVerbBuilder.presentParticipleBuilder(combo.subordinateSentenceType())
        val participleForm = NounBuilder
            .ofPhrasalBuilder(participleBuilder, subordinateVerbBuilder.extractSoftOffset())
            .possessiveSeptikForm(firstForm.person, firstForm.number, subordinateSeptik).raw
        val answer = "${subordinatePrefix}${participleForm} ${mainClause}"

        return TaskItem(
            "(${descriptor})\n${pattern}",
            listOf(
                answer
            ),
            translations = combo.collectTranslations(),
        )
    }

    private fun collectTasks(generator: (taskId: Int) -> TaskItem): GetTasks {
        val tasks = mutableListOf<TaskItem>()
        for (taskId in 1..taskCount) {
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

    fun genPresentParticiple() = collectTasks {
        val combo = kCombos.random()
        buildTask(combo)
    }
}