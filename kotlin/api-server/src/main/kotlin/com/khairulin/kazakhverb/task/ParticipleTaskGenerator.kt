package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.GrammarForm
import com.khairulin.kazakhverb.grammar.NounBuilder
import com.khairulin.kazakhverb.grammar.PronounDeclension
import com.khairulin.kazakhverb.grammar.SentenceType
import com.khairulin.kazakhverb.grammar.Septik
import com.khairulin.kazakhverb.grammar.VerbBuilder
import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.response.TaskItem

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

    enum class Shak {
        presentTransitive,
        past,
        presentContinuousTur,
        ;

        fun apply(builder: VerbBuilder, grammarForm: GrammarForm, sentenceType: SentenceType): String {
            return when(this) {
                presentTransitive -> builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)
                past -> builder.past(grammarForm.person, grammarForm.number, sentenceType)
                presentContinuousTur -> builder.presentContinuousForm(grammarForm.person, grammarForm.number, sentenceType, AuxVerbProvider.turuBuilder)
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

        fun mainPrefixString(mainGrammarForm: GrammarForm): String {
            if (mainPrefix?.text == "өз[ENDING]") {
                val ozForm = PronounDeclension.getOzForm(mainGrammarForm)
                return "${ozForm} "
            }
            return stringFragmentToString(mainPrefix)
        }

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
            VerbInfo("білу", translation = "знать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "не",
                listOf(
                    Pair("не", "что")
                )
            ),
            mainVerbNegative = true,
        ),
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("есту", translation = "слышать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "бірдеңе",
                listOf(
                    Pair("бірдеңе", "что-то")
                )
            ),
            mainVerbNegative = true,
        ),
        // 3 - [ештеңе істемейсің] [оны] көріп тұрмын
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("көру", translation = "видеть"),
            Shak.presentContinuousTur,
            subordinatePrefix = StringFragment(
                "ештеңе",
                listOf(
                    Pair("ештеңе", "ничего")
                )
            ),
            subordinateVerbNegative = true,
        ),
        // 4 - [бұл жұмысты істейсіңдер] [оларға] көмектесемін
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Barys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("көмектесу", translation = "помогать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "бұл жұмысты",
                listOf(
                    Pair("жұмыс", "работа")
                )
            ),
        ),
        // 5 - [ертең не істейтінін] [оны] мен білемін
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("білу", translation = "знать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "ертең не",
                listOf(
                    Pair("ертең", "завтра"),
                    Pair("не", "что"),
                )
            ),
        ),
        // 6 - [онда жұмыс істейді] [оны] сендер білмейсіңдер
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("жұмыс істеу", translation = "работать"),
            VerbInfo("білу", translation = "знать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "онда",
                listOf(
                    Pair("онда", "там"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 7 - [бүгін келейді] [оны] білмедім
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("келу", translation = "приезжать"),
            VerbInfo("білу", translation = "знать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "бүгін",
                listOf(
                    Pair("бүгін", "сегодня"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 8 - [бүгін келмедіңіз] [оны] білмедік
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("келу", translation = "приезжать"),
            VerbInfo("білу", translation = "знать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "бүгін",
                listOf(
                    Pair("бүгін", "сегодня"),
                )
            ),
            subordinateVerbNegative = true,
            mainVerbNegative = true,
        ),
        // 9 - [қалаға барасың] [оны] білмедім
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("бару", translation = "ехать"),
            VerbInfo("білу", translation = "знать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "қалаға",
                listOf(
                    Pair("қала", "город"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 10 - [ертең не сатып аламын] [оны] ойлаймын
        // orig: Ертең жалақы алған кезде не сатып алатынымды ойластырып, қиялыммен байимын.
        Combo(
            GrammarFormAffinity.matchRequired,
            Septik.Barys,
            VerbInfo("сатып алу", translation = "покупать"),
            VerbInfo("ойлау", translation = "думать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "ертең не",
                listOf(
                    Pair("ертең", "завтра"),
                    Pair("не", "что"),
                )
            ),
        ),
        // 11 - [ол келеді] [оны] мен күттім
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("келу", translation = "приезжать"),
            VerbInfo("күту", translation = "ждать"),
            Shak.past,
        ),
        // 12 - [сен не айтаcың] [оны] түсінбедім
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("айту", translation = "сказать"),
            VerbInfo("түсіну", translation = "понимать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "не",
                listOf(
                    Pair("не", "что"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 13 - [ол ашуланып шыға келеді] [оны] білемін
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("келу"),
            VerbInfo("білу", translation = "знать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "ашуланып шыға",
                listOf(
                    Pair("ашулану", "злиться"),
                    Pair("шығу", "выходить"),
                )
            ),
        ),
        // 14 - [ол тура мұғалімнің өзінен оқиды] [оны] ол білді
        // orig: Оқығанда, тура Қабдөш мұғалімнің өзінен оқитынын білді.
        Combo(
            GrammarFormAffinity.matchRequired,
            Septik.Tabys,
            VerbInfo("оқу", translation = "учиться"),
            VerbInfo("білу", translation = "знать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "тура мұғалімнің өзінен",
                listOf(
                    Pair("тура", "прямо"),
                    Pair("мұғалім", "учитель"),
                    Pair("өзінен", "от него самого"),
                )
            )
        ),
        // 15 - [мен жылап қайтамын] [оны] ескермедi
        // orig: Несиенiң күлiп келiп, жылап қайтатынын ескермедi.
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("қайту", translation = "возвращаться"),
            VerbInfo("ескеру", translation = "обращать внимание"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "жылап",
                listOf(
                    Pair("жылау", "плакать"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 16 - [неге солай ойлайды] [оны] өзі білмейді
        // orig: Әйткенмен соны ойлайды, неге ойлайтынын өзі де білмейді
        Combo(
            GrammarFormAffinity.matchRequired,
            Septik.Tabys,
            VerbInfo("ойлау", translation = "думать"),
            VerbInfo("білу", translation = "знать"),
            Shak.past,
            subordinatePrefix = StringFragment(
                "неге солай",
                listOf(
                    Pair("неге", "почему"),
                    Pair("солай", "так"),
                )
            ),
            mainPrefix = StringFragment(
                "өз[ENDING]",
            ),
            mainVerbNegative = true,
        ),
        // 17 - [өсе келе керемет істер істейді] [оған] сенеді
        // orig: Өсе келе керемет істер істейтініне, атағы шығатынына, көптің құрметіне ие болатынына сенеді.
        Combo(
            GrammarFormAffinity.matchRequired,
            Septik.Barys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("сену", translation = "верить"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "өсе келе керемет істер",
                listOf(
                    Pair("өсе келе", "когда вырасту/вырастешь/вырастет/…"),
                    Pair("керемет", "великий"),
                    Pair("іс", "дело"),
                )
            ),
        ),
    )

    private fun buildTask(combo: Combo): TaskItem {
        val (firstForm, secondForm) = combo.affinity.getRandomGrammarFormPair()
        val subordinateSeptik = combo.subordinateSeptik
        val descriptor = "причастие наст. вр., ${firstForm.ruShort}, ${subordinateSeptik.ruShort}"
        val subordinateVerbBuilder = combo.subordinateVerb.builder()
        val hintVerbForm = subordinateVerbBuilder.presentTransitiveForm(
            firstForm.person,
            firstForm.number,
            combo.subordinateSentenceType()
        ).raw
        val mainVerbForm = combo.shak.apply(
            combo.mainVerb.builder(),
            secondForm,
            combo.mainSentenceType(),
        )
        val mainPrefix = combo.mainPrefixString(secondForm)
        val mainClause = "${mainPrefix}${mainVerbForm}"
        val subordinatePrefix = combo.subordinatePrefixString()
        val pattern = "[${firstForm.pronoun} ${subordinatePrefix}${hintVerbForm}] ${mainClause}"

        val participleBuilder = subordinateVerbBuilder.presentParticipleBuilder(combo.subordinateSentenceType())
        val participleForm = NounBuilder
            .ofPhrasalBuilder(participleBuilder, subordinateVerbBuilder.extractSoftOffset())
            .possessiveSeptikForm(firstForm.person, firstForm.number, subordinateSeptik).raw
        val answer = "${subordinatePrefix}${participleForm} ${mainClause}"

        return TaskItem(
            "(${descriptor})\n`${pattern}`",
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