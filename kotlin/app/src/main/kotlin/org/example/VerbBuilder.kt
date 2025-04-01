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

    private fun getPersAffix1ExceptThirdPerson(person: GrammarPerson, number: GrammarNumber, formSoftOffset: Int): String {
        if (person == GrammarPerson.Third) {
            return ""
        }
        return Rules.VERB_PERS_AFFIXES1[person]!![number]!![formSoftOffset]
    }

    private fun getPresentContinuousBase(): String {
        if (Rules.VERB_PRESENT_CONT_EXCEPTION_U_SET.contains(verbDictForm) && !forceExceptional) {
            return StrManip.replaceLast(verbBase, 'у')
        }
        return verbBase
    }

    private fun getPresentContinousAffix(): String {
        return when {
            Rules.VERB_PRESENT_CONT_EXCEPTION_A_SET.contains(verbDictForm) -> "а"
            Rules.VERB_PRESENT_CONT_EXCEPTION_E_SET.contains(verbDictForm) -> "е"
            else -> VerbSuffix.getYpip(char = baseLast, softOffset = softOffset)
        }
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

    private fun presentSimpleContinuousCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (contContext == null) {
            return PhrasalBuilder()
        }
        val persAffix = getPersAffix1ExceptThirdPerson(person, number, formSoftOffset = softOffset)
        return PhrasalBuilder()
            .verbBase(contContext)
            .personalAffix(persAffix)
    }

    fun presentSimpleContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (contContext == null) {
            return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }

        return when (sentenceType) {
            SentenceType.Statement -> {
                presentSimpleContinuousCommonBuilder(person, number)
                    .build()
            }
            SentenceType.Negative -> {
                val affix = VerbSuffix.getGangenKanken(char = baseLast, softOffset = softOffset)

                // parameters of "жоқ", not of the verb base
                val gokLast: Char = 'қ'
                val gokSoftOffset = 0

                val persAffix = PersAffix.getPersAffix1(person, number, gokLast, gokSoftOffset)
                PhrasalBuilder()
                    .verbBase(verbBase)
                    .tenseAffix(affix)
                    .space()
                    .negation("жоқ")
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    presentSimpleContinuousCommonBuilder(person, number)
                ).build()
            }
            else -> PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }
    }

    fun presentContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, auxBuilder: VerbBuilder, negateAux: Boolean = true): Phrasal {
        if (auxBuilder.contContext == null) {
            return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }

        val aeException = (
                Rules.VERB_PRESENT_CONT_EXCEPTION_A_SET.contains(verbDictForm) ||
                Rules.VERB_PRESENT_CONT_EXCEPTION_E_SET.contains(verbDictForm)
        )
        val forbidden = aeException && auxBuilder.verbDictForm != Rules.VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED

        return if (sentenceType != SentenceType.Negative || negateAux) {
            val contBase = getPresentContinuousBase()
            val affix = getPresentContinousAffix()
            val auxVerbPhrasal = auxBuilder.presentSimpleContinuousForm(person, number, sentenceType)
            PhrasalBuilder()
                .verbBase(contBase)
                .tenseAffix(affix)
                .space()
                .auxVerb(phrasal = auxVerbPhrasal)
                .setForbidden(forbidden = forbidden)
                .build()
        } else {
            val base = genericBaseModifier(nc = true, yp = false)
            val particle = Question.getQuestionParticle(char = base.last, softOffset = softOffset)
            val affix = "й"
            val auxVerbPhrasal = auxBuilder.presentSimpleContinuousForm(person, number, SentenceType.Statement)
            PhrasalBuilder()
                .verbBase(base.base)
                .negation(particle)
                .tenseAffix(affix)
                .space()
                .auxVerb(phrasal = auxVerbPhrasal)
                .setForbidden(forbidden = forbidden)
                .build()
        }
    }

    private fun pastCommonBuilder(): PhrasalBuilder {
        val pastBase = genericBaseModifier(nc = true, yp = false)
        val affix = VerbSuffix.getDydiTyti(pastBase.last, softOffset)
        return PhrasalBuilder()
            .verbBase(pastBase.base)
            .tenseAffix(affix)
    }

    fun past(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val persAffix = Rules.VERB_PERS_AFFIXES2[person]!![number]!![softOffset]
        return when (sentenceType) {
            SentenceType.Statement -> pastCommonBuilder().personalAffix(persAffix).build()
            SentenceType.Negative -> {
                val pastBase = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(pastBase.last, softOffset)
                val affix = Rules.DYDI[softOffset]
                return PhrasalBuilder()
                    .verbBase(pastBase.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question ->
                buildQuestionForm(pastCommonBuilder().personalAffix(persAffix)).build()
            else -> PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }
    }
}