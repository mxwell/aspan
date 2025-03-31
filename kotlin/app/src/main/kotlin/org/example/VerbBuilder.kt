package org.example

class VerbBuilder(private val verbDictForm: String, private val forceExceptional: Boolean = false) {

    companion object {
        fun softToOffset(soft: Boolean): Int {
            return if (soft) 1 else 0
        }

        fun validateVerb(verbDictForm: String): Boolean {
            var count = 0
            for (char in verbDictForm) {
                count++
                if (count > 100) {
                    println("verb is too long")
                    return false
                }
                if (char != ' ' && char != '-' && !char.isLowerCase()) {
                    println("verb is not lowercase: char $char, verb $verbDictForm")
                    return false
                }
            }
            if (count < 2) {
                println("verb is too short")
                return false
            }
            val lastChar = verbDictForm.last()
            if (lastChar != 'у' && lastChar != 'ю') {
                println("bad last char in verb")
                return false
            }
            return true
        }

        fun isVerbException(verbLastWord: String): Boolean {
            return Rules.VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET.contains(verbLastWord)
        }

        fun getOptExceptVerbMeanings(verbLastWord: String): List<List<String>>? {
            return Rules.OPT_EXCEPT_VERB_MEANINGS[verbLastWord]
        }

        fun isVerbException2(verbLastWord: String): Boolean {
            return Rules.VERB_PRESENT_TRANSITIVE_EXCEPTIONS2_SET.contains(verbLastWord)
        }
    }

    data class BaseAndLast(val base: String, val last: Char) {
        companion object {
            fun ofBase(base: String): BaseAndLast {
                return BaseAndLast(base, base.last())
            }

            fun ofBaseAndLastReplacement(origBase: String, lastReplacement: Char): BaseAndLast {
                return BaseAndLast(origBase.dropLast(1) + lastReplacement, lastReplacement)
            }
        }
    }

    private val verbBase: String
    private val regularVerbBase: String
    private val soft: Boolean
    private val softOffset: Int
    private val needsYaSuffix: Boolean
    private val baseLast: Char
    private val contContext: String?
    var optativeAuxBuilder: VerbBuilder? = null

    init {
        require(validateVerb(verbDictForm)) { "Invalid verb dictionary form" }

        val verbLastWord = StrManip.getLastWord(verbDictForm)
        var base = verbDictForm.dropLast(1)

        regularVerbBase = base
        soft = Phonetics.wordIsSoft(verbLastWord)
        softOffset = softToOffset(soft)

        var needsYaSuffix = false

        base = when {
            isVerbException(verbLastWord) || (getOptExceptVerbMeanings(verbLastWord) != null && forceExceptional) ->
                "$base${Rules.VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[softOffset]}"
            isVerbException2(verbLastWord) -> "${base}й${Rules.VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[softOffset]}"
            verbDictForm.endsWith("ю") -> {
                if (verbDictForm.endsWith("ию")) {
                    needsYaSuffix = !soft
                    base
                } else {
                    "${base}й"
                }
            }
            else -> base
        }

        verbBase = base
        this.needsYaSuffix = needsYaSuffix
        baseLast = base.last()
        contContext = Rules.VERB_PRESENT_CONT_BASE_MAP[verbDictForm]
    }

    private fun presentTransitiveSuffix(): String = when {
        needsYaSuffix -> "я"
        Phonetics.genuineVowel(baseLast) -> "й"
        soft -> "е"
        else -> "а"
    }

    private fun mergeBaseWithVowelAffix(origBase: String, origAffix: String): PhrasalBuilder {
        var base = origBase
        var affix = origAffix
        when {
            base.endsWith("й") && affix.startsWith("а") -> {
                base = base.dropLast(1)
                affix = "я${affix.drop(1)}"
            }
            (base.endsWith("ы") || base.endsWith("і")) && affix.startsWith("й") -> {
                base = base.dropLast(1)
                affix = "и${affix.drop(1)}"
            }
        }
        return PhrasalBuilder().verbBase(base).tenseAffix(affix)
    }

    private fun genericBaseModifier(nc: Boolean, yp: Boolean): BaseAndLast {
        require(!(nc && yp)) { "genericBaseModifier called with nc and yp simultaneously" }
        return when {
            nc -> Rules.VERB_EXCEPTION_ADD_VOWEL_MAP[verbDictForm]?.let { BaseAndLast.ofBase(it) }
                ?: Rules.VERB_LAST_NEGATIVE_CONVERSION[verbBase.last()]?.let { BaseAndLast.ofBaseAndLastReplacement(verbBase, it) }
                ?: BaseAndLast(verbBase, baseLast)
            yp && Rules.VERB_PRESENT_CONT_EXCEPTION_U_SET.contains(verbDictForm) && !forceExceptional ->
                BaseAndLast.ofBaseAndLastReplacement(regularVerbBase, 'у')
            else -> BaseAndLast(verbBase, baseLast)
        }
    }

    private fun buildQuestionFormGeneric(builder: PhrasalBuilder, questionSoft: Boolean): PhrasalBuilder {
        val last = builder.getLastItem()
        val particle = Question.getQuestionParticle(last, softToOffset(questionSoft))
        return builder.space().questionParticle(particle).punctuation("?")
    }

    private fun buildQuestionForm(builder: PhrasalBuilder): PhrasalBuilder =
        buildQuestionFormGeneric(builder, soft)

    private fun appendPresentTransitivePersAffix(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, builder: PhrasalBuilder): PhrasalBuilder {
        val persAffix = if (sentenceType != SentenceType.Question || person != GrammarPerson.Third) {
            Rules.VERB_PERS_AFFIXES1[person]?.get(number)?.get(softOffset) ?: ""
        } else ""
        return builder.personalAffix(persAffix)
    }

    private fun presentTransitiveCommonBuilder(): PhrasalBuilder =
        mergeBaseWithVowelAffix(verbBase, presentTransitiveSuffix())

    fun presentTransitiveForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement ->
                appendPresentTransitivePersAffix(person, number, sentenceType, presentTransitiveCommonBuilder()).build()
            SentenceType.Negative -> {
                val pastBase = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(pastBase.last, softOffset)
                val affix = "й"
                appendPresentTransitivePersAffix(person, number, sentenceType, PhrasalBuilder().verbBase(pastBase.base).negation(particle).tenseAffix(affix)).build()
            }
            SentenceType.Question ->
                buildQuestionForm(appendPresentTransitivePersAffix(person, number, sentenceType, presentTransitiveCommonBuilder())).build()
            else -> PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }
    }
}