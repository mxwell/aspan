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
        presentContinuousOtyr,
        presentContinuousTur,
        ;

        fun apply(builder: VerbBuilder, grammarForm: GrammarForm, sentenceType: SentenceType): String {
            return when(this) {
                presentTransitive -> builder.presentTransitiveForm(grammarForm.person, grammarForm.number, sentenceType)
                past -> builder.past(grammarForm.person, grammarForm.number, sentenceType)
                presentContinuousOtyr -> builder.presentContinuousForm(grammarForm.person, grammarForm.number, sentenceType, AuxVerbProvider.otyruBuilder)
                presentContinuousTur -> builder.presentContinuousForm(grammarForm.person, grammarForm.number, sentenceType, AuxVerbProvider.turuBuilder)
            }.raw
        }
    }

    enum class TenseCompat {
        onlyPresent,
        onlyPast,
        presentAndPast,
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
        val compat: TenseCompat = TenseCompat.presentAndPast,
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
        // 1 - [ол не істейді] [оны] білмеймін
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("істеу", translation = "делать"),
            VerbInfo("білу", translation = "знать"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "не",
                listOf(
                    Pair("не", "что"),
                )
            ),
            mainVerbNegative = true,
        ),
        // 2 - [ол бірдеңе істейді] [оны] естімедім
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
            compat = TenseCompat.onlyPresent,
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
            compat = TenseCompat.onlyPresent,
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
            compat = TenseCompat.onlyPresent,
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
        // 8 - [бүгін келмейсіз] [оны] білмедік
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
            compat = TenseCompat.onlyPresent,
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
            compat = TenseCompat.onlyPresent,
        ),
        // 18 - [мен айттым] [оны] түсінесің
        // orig: Айтқанымды түсіндің бе?
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("айту", translation = "сказать"),
            VerbInfo("түсіну", translation = "понимать"),
            Shak.presentTransitive,
        ),
        // 19 - [мен Ахметпен таныстым] [оған] қатты қуанамын
        // orig: Сізбен танысқаныма өте қуаныштымын!
        Combo(
            GrammarFormAffinity.matchRequired,
            Septik.Barys,
            VerbInfo("танысу", translation = "знакомиться"),
            VerbInfo("қуану", translation = "радоваться"),
            Shak.presentTransitive,
            subordinatePrefix = StringFragment(
                "Ахметпен",
                listOf(
                    Pair("Ахметпен", "с Ахметом"),
                )
            ),
            mainPrefix = StringFragment(
                "қатты",
                listOf(
                    Pair("қатты", "сильно"),
                )
            )
        ),
        // 20 - [ол ренжіді] [оны] сезіп отырмын
        // orig: Қапанның ренжігенін Сақып сезіп отыр екен.
        Combo(
            GrammarFormAffinity.mismatchRequired,
            Septik.Tabys,
            VerbInfo("ренжу", translation = "расстраиваться"),
            VerbInfo("сезу", translation = "чувствовать"),
            Shak.presentContinuousOtyr,
        ),
    )
    private val kPresentCombos: List<Combo> by lazy {
        kCombos.filter {
            it.compat == TenseCompat.presentAndPast || it.compat == TenseCompat.onlyPresent
        }
    }
    private val kPastCombos: List<Combo> by lazy {
        kCombos.filter {
            it.compat == TenseCompat.presentAndPast || it.compat == TenseCompat.onlyPast
        }
    }

    private fun buildTask(combo: Combo, presentParticiple: Boolean): TaskItem {
        val (firstForm, secondForm) = combo.affinity.getRandomGrammarFormPair()
        val subordinateSeptik = combo.subordinateSeptik
        val title = if (presentParticiple) {
            "причастие наст. вр."
        } else {
            "причастие прош. вр."
        }
        val descriptor = "${title}, ${firstForm.ruShort}, ${subordinateSeptik.ruShort}"
        val subordinateVerbBuilder = combo.subordinateVerb.builder()
        val subordinateSentenceType = combo.subordinateSentenceType()
        val hintVerbForm = if (presentParticiple) {
            subordinateVerbBuilder.presentTransitiveForm(
                firstForm.person,
                firstForm.number,
                subordinateSentenceType
            )
        } else {
            subordinateVerbBuilder.past(firstForm.person, firstForm.number, subordinateSentenceType)
        }.raw
        val mainVerbForm = combo.shak.apply(
            combo.mainVerb.builder(),
            secondForm,
            combo.mainSentenceType(),
        )
        val mainPrefix = combo.mainPrefixString(secondForm)
        val mainClause = "${mainPrefix}${mainVerbForm}"
        val subordinatePrefix = combo.subordinatePrefixString()
        val pattern = "[${firstForm.pronoun} ${subordinatePrefix}${hintVerbForm}] ${mainClause}"

        val participleBuilder = if (presentParticiple) {
            subordinateVerbBuilder.presentParticipleBuilder(subordinateSentenceType)
        } else {
            subordinateVerbBuilder.pastParticipleBuilder(subordinateSentenceType)
        }
        val participleForm = NounBuilder
            .ofPhrasalBuilder(participleBuilder, subordinateVerbBuilder.extractSoftOffset())
            .possessiveSeptikForm(firstForm.person, firstForm.number, subordinateSeptik).raw
        val answer = "${subordinatePrefix}${participleForm} ${mainClause}"

        return TaskItem(
            "(${descriptor})\n`${pattern}`\n",
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
        val combo = kPresentCombos.random()
        buildTask(combo, presentParticiple = true)
    }

    fun genPastParticiple() = collectTasks {
        val combo = kPastCombos.random()
        buildTask(combo, presentParticiple = false)
    }
}