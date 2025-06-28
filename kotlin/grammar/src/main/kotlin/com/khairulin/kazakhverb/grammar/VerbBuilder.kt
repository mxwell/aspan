package com.khairulin.kazakhverb.grammar

class VerbBuilder(val verbDictForm: String, private val forceExceptional: Boolean = false) {

    companion object {
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
    var canAuxBuilder: VerbBuilder? = null

    init {
        require(validateVerb(verbDictForm)) { "Invalid verb dictionary form" }

        val verbLastWord = StrManip.getLastWord(verbDictForm)
        var base = verbDictForm.dropLast(1)

        regularVerbBase = base
        soft = Phonetics.wordIsSoft(verbLastWord)
        softOffset = Phonetics.softToOffset(soft)

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

    fun extractSoftOffset() = softOffset

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

    private fun getPerfectParticipleAffix(): String =
        VerbSuffix.getYpip(char = baseLast, softOffset = softOffset)

    private fun getPresentContinousAffix(): String {
        return when {
            Rules.VERB_PRESENT_CONT_EXCEPTION_A_SET.contains(verbDictForm) -> "а"
            Rules.VERB_PRESENT_CONT_EXCEPTION_E_SET.contains(verbDictForm) -> "е"
            else -> getPerfectParticipleAffix()
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
                ?: Rules.VERB_LAST_NEGATIVE_CONVERSION[verbBase.last()]?.let {
                    BaseAndLast.ofBaseAndLastReplacement(
                        verbBase,
                        it
                    )
                }
                ?: BaseAndLast(verbBase, baseLast)
            yp && Rules.VERB_PRESENT_CONT_EXCEPTION_U_SET.contains(verbDictForm) && !forceExceptional ->
                BaseAndLast.ofBaseAndLastReplacement(regularVerbBase, 'у')
            else -> BaseAndLast(verbBase, baseLast)
        }
    }

    private fun buildQuestionFormGeneric(builder: PhrasalBuilder, questionSoft: Boolean): PhrasalBuilder {
        val last = builder.getLastItem()
        val particle = Question.getQuestionParticle(last, Phonetics.softToOffset(questionSoft))
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
            SentenceType.Question -> {
                buildQuestionForm(
                    appendPresentTransitivePersAffix(
                        person,
                        number,
                        sentenceType,
                        presentTransitiveCommonBuilder()
                    )
                ).build()
            }
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
        }
    }

    private fun remotePastCommonBuilder(): PhrasalBuilder {
        val pastBase = genericBaseModifier(nc = true, yp = false)
        val affix = VerbSuffix.getGangenKanken(char = pastBase.last, softOffset = softOffset)
        return PhrasalBuilder()
            .verbBase(pastBase.base)
            .tenseAffix(affix)
    }

    fun remotePast(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, negateAux: Boolean = true): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement -> {
                val builder = remotePastCommonBuilder()
                val affixLast = builder.getLastItem()
                val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
                builder
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Negative -> {
                if (negateAux) {
                    val builder = remotePastCommonBuilder()

                    // parameters of "жоқ", not of the verb base
                    val gokLast: Char = 'қ'
                    val gokSoftOffset = 0

                    val persAffix = PersAffix.getPersAffix1(person, number, gokLast, gokSoftOffset)
                    builder
                        .space()
                        .negation("жоқ")
                        .personalAffix(persAffix)
                        .build()
                } else {
                    val pastBase = genericBaseModifier(nc = true, yp = false)
                    val particle = Question.getQuestionParticle(char = pastBase.last, softOffset = softOffset)
                    val particleLast = particle.last()
                    val affix = VerbSuffix.getGangenKanken(char = particleLast, softOffset = softOffset)
                    val affixLast = affix.last()
                    val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
                    PhrasalBuilder()
                        .verbBase(pastBase.base)
                        .negation(particle)
                        .tenseAffix(affix)
                        .personalAffix(persAffix)
                        .build()
                }
            }
            SentenceType.Question -> {
                val builder = remotePastCommonBuilder()
                val affixLast = builder.getLastItem()
                val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
                buildQuestionForm(
                    builder.personalAffix(persAffix)
                ).build()
            }
        }
    }

    private fun pastUncertainCommonBuilder(): PhrasalBuilder {
        val base = genericBaseModifier(nc = false, yp = true)
        val baseLast = base.last
        val affix = VerbSuffix.getYpip(baseLast, softOffset)
        return PhrasalBuilder()
            .verbBase(base.base)
            .tenseAffix(affix)
    }

    fun pastUncertainTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val persAffix = PersAffix.getPersAffix3(person, number, softOffset)

        return when (sentenceType) {
            SentenceType.Statement -> {
                pastUncertainCommonBuilder()
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Negative -> {
                val baseAndLast = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(baseAndLast.last, softOffset)
                val particleLast = particle.last()
                val affix = VerbSuffix.getYpip(particleLast, softOffset)
                PhrasalBuilder()
                    .verbBase(baseAndLast.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    pastUncertainCommonBuilder()
                        .personalAffix(persAffix)
                ).build()
            }
        }
    }

    private fun conditionalMoodCommonBuilder(): PhrasalBuilder {
        val pastBase = genericBaseModifier(nc = true, yp = false)
        val affix = Rules.SASE[softOffset]
        return PhrasalBuilder()
            .verbBase(pastBase.base)
            .tenseAffix(affix)
    }

    fun conditionalMood(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val persAffix = Rules.VERB_PERS_AFFIXES2[person]!![number]!![softOffset]
        return when (sentenceType) {
            SentenceType.Statement -> {
                conditionalMoodCommonBuilder()
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Negative -> {
                val pastBase = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(char = pastBase.last, softOffset = softOffset)
                val affix = Rules.SASE[softOffset]
                PhrasalBuilder()
                    .verbBase(pastBase.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    conditionalMoodCommonBuilder()
                        .personalAffix(persAffix)
                ).build()
            }
        }
    }

    private fun imperativeMoodCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        val nc = (
                (person == GrammarPerson.Second && number == GrammarNumber.Singular) ||
                        person == GrammarPerson.Third
                )
        val baseAndLast = genericBaseModifier(nc = nc, yp = false)
        val affix = VerbSuffix.getImperativeAffix(
            person = person,
            number = number,
            char = baseAndLast.last,
            softOffset = softOffset
        )
        return mergeBaseWithVowelAffix(origBase = baseAndLast.base, origAffix = affix)
    }

    fun imperativeMood(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement -> {
                imperativeMoodCommonBuilder(person, number)
                    .build()
            }
            SentenceType.Negative -> {
                val pastBase = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(char = pastBase.last, softOffset = softOffset)
                val particleLast = particle.last()
                val affix = VerbSuffix.getImperativeAffix(
                    person = person,
                    number = number,
                    char = particleLast,
                    softOffset = softOffset
                )
                PhrasalBuilder()
                    .verbBase(pastBase.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    imperativeMoodCommonBuilder(person, number)
                ).build()
            }
        }
    }

    private fun createOptativeAuxBuilder(): VerbBuilder {
        return optativeAuxBuilder ?: run {
            val builder = VerbBuilder(verbDictForm = "келу")
            optativeAuxBuilder = builder
            builder
        }
    }

    private fun getOptativeAux(sentenceType: SentenceType): Phrasal {
        return createOptativeAuxBuilder().presentTransitiveForm(
            GrammarPerson.Third,
            GrammarNumber.Singular, sentenceType
        )
    }

    fun optativeMood(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val baseAndLast = genericBaseModifier(nc = true, yp = false)
        val affix = VerbSuffix.getGygiKyki(char = baseAndLast.last, softOffset = softOffset)
        val persAffix = Rules.VERB_WANT_PERS_AFFIXES[person]!![number]!![softOffset]
        val aux = getOptativeAux(sentenceType = sentenceType)
        return PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix)
            .personalAffix(persAffix)
            .space()
            .auxVerb(phrasal = aux)
            .build()
    }

    fun optativeMoodInPastTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val baseAndLast = genericBaseModifier(nc = true, yp = false)
        val affix = VerbSuffix.getGygiKyki(char = baseAndLast.last, softOffset = softOffset)
        val persAffix = Rules.VERB_WANT_PERS_AFFIXES[person]!![number]!![softOffset]
        val aux = createOptativeAuxBuilder().past(
            GrammarPerson.Third,
            GrammarNumber.Singular,
            sentenceType
        )
        return PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix)
            .personalAffix(persAffix)
            .space()
            .auxVerb(phrasal = aux)
            .build()
    }

    private fun createCanAuxBuilder(): VerbBuilder {
        return canAuxBuilder ?: run {
            val builder = VerbBuilder(verbDictForm = "алу")
            canAuxBuilder = builder
            builder
        }
    }

    fun canClause(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val aux = createCanAuxBuilder().presentTransitiveForm(person, number, sentenceType)
        return this.presentTransitiveCommonBuilder()
            .space()
            .auxVerb(phrasal = aux)
            .build()
    }

    fun canClauseInPastTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        val aux = createCanAuxBuilder().past(person, number, sentenceType)
        return this.presentTransitiveCommonBuilder()
            .space()
            .auxVerb(phrasal = aux)
            .build()
    }

    fun canClauseInPresentContinuous(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, auxBuilder: VerbBuilder): Phrasal {
        val aux = createCanAuxBuilder().presentContinuousForm(person, number, sentenceType, auxBuilder, negateAux = false)
        return this.presentTransitiveCommonBuilder()
            .space()
            .auxVerb(phrasal = aux)
            .build()
    }

    private val koruAuxBuilder: VerbBuilder by lazy {
        VerbBuilder("көру")
    }

    private fun ofTenseOrThrow(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, tense: VerbTense): Phrasal {
        return when (tense) {
            VerbTense.TensePresentTransitive -> presentTransitiveForm(person, number, sentenceType)
            VerbTense.TensePast -> past(person, number, sentenceType)
            VerbTense.MoodOptative -> optativeMood(person, number, sentenceType)
            else -> throw IllegalThreadStateException("tense ${tense} not supported by ofTenseOrThrow()")
        }
    }

    fun koruClauseOfTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, tense: VerbTense): Phrasal {
        val contBase = getPresentContinuousBase()
        val affix = getPerfectParticipleAffix()
        val auxVerbPhrasal = koruAuxBuilder.ofTenseOrThrow(person, number, sentenceType, tense)
        return PhrasalBuilder()
            .verbBase(contBase)
            .tenseAffix(affix)
            .space()
            .auxVerb(phrasal = auxVerbPhrasal)
            .build()
    }

    private val jazdauAuxBuidler: VerbBuilder by lazy {
        VerbBuilder("жаздау")
    }

    fun jazdauClause(person: GrammarPerson, number: GrammarNumber, auxBuilder: VerbBuilder): Phrasal {
        val contBase = getPresentContinuousBase()
        val affix = getPerfectParticipleAffix()
        val firstAuxVerbPhrasal = auxBuilder.presentTransitiveCommonBuilder().build()
        val secondAuxVerbPhrasal = jazdauAuxBuidler.past(person, number, SentenceType.Statement)
        return PhrasalBuilder()
            .verbBase(contBase)
            .tenseAffix(affix)
            .space()
            .auxVerb(phrasal = firstAuxVerbPhrasal)
            .space()
            .auxVerb(phrasal = secondAuxVerbPhrasal)
            .build()
    }

    private fun possibleFutureSuffix(): String = when {
        Phonetics.genuineVowel(baseLast) -> "р"
        soft -> "ер"
        else -> "ар"
    }

    private fun possibleFutureCommonBuilder(): PhrasalBuilder {
        val affix = possibleFutureSuffix()
        if (baseLast == 'й' && affix == "ар") {
            return PhrasalBuilder()
                .verbBase(verbBase.dropLast(1))
                .tenseAffix("яр")
        }
        return PhrasalBuilder()
            .verbBase(verbBase)
            .tenseAffix(affix)
    }

    private fun possibleFutureCommonWithPersAffixBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        val builder = possibleFutureCommonBuilder()
        val affixLast = builder.getLastItem()
        val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
        return builder
            .personalAffix(persAffix)
    }

    fun possibleFutureForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement -> {
                possibleFutureCommonWithPersAffixBuilder(person, number)
                    .build()
            }
            SentenceType.Negative -> {
                val base = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(char = base.last, softOffset = softOffset)
                val affix = 'с'
                val persAffix = PersAffix.getPersAffix1(person, number, affix, softOffset)
                PhrasalBuilder()
                    .verbBase(base.base)
                    .negation(particle)
                    .tenseAffix(affix.toString())
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    possibleFutureCommonWithPersAffixBuilder(person, number)
                ).build()
            }
        }
    }

    private fun pastTransitiveSuffix(prevChar: Char) = when {
        needsYaSuffix -> Rules.YATYN
        Phonetics.genuineVowel(prevChar) -> Rules.YTYNYTIN[softOffset]
        else -> Rules.ATYNETYN[softOffset]
    }

    private fun presentParticipleCommonBuilder() =
        mergeBaseWithVowelAffix(
            verbBase,
            pastTransitiveSuffix(baseLast)
        )

    fun presentParticipleBuilder(sentenceType: SentenceType): PhrasalBuilder {
        return when (sentenceType) {
            SentenceType.Statement -> presentParticipleCommonBuilder()
            SentenceType.Negative -> {
                val base = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(base.last, softOffset)
                val affix = pastTransitiveSuffix(particle.last())
                PhrasalBuilder()
                    .verbBase(base.base)
                    .negation(particle)
                    .tenseAffix(affix)
            }
            SentenceType.Question -> buildQuestionForm(presentParticipleCommonBuilder())
        }
    }

    fun presentParticiple(sentenceType: SentenceType) = presentParticipleBuilder(sentenceType).build()

    fun pastParticipleBuilder(sentenceType: SentenceType): PhrasalBuilder {
        return when (sentenceType) {
            SentenceType.Statement -> remotePastCommonBuilder()
            SentenceType.Negative -> {
                val base = genericBaseModifier(nc = true, yp = false)
                val particle = Question.getQuestionParticle(base.last, softOffset)
                val affix = VerbSuffix.getGangenKanken(baseLast, softOffset)
                PhrasalBuilder()
                    .verbBase(base.base)
                    .negation(particle)
                    .tenseAffix(affix)
            }
            SentenceType.Question -> buildQuestionForm(remotePastCommonBuilder())
        }
    }

    fun pastParticiple(sentenceType: SentenceType) = pastParticipleBuilder(sentenceType).build()

    private fun pastTransitiveCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        val builder = presentParticipleCommonBuilder()
        val affixLast = builder.getLastItem()
        val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
        return builder.personalAffix(persAffix)
    }

    private fun baseNegativeBuilder(): PhrasalBuilder {
        val base = genericBaseModifier(nc = true, yp = false)
        val particle = Question.getQuestionParticle(char = base.last, softOffset = softOffset)
        return PhrasalBuilder()
            .verbBase(base.base)
            .negation(particle)
    }

    fun pastTransitiveTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement -> {
                pastTransitiveCommonBuilder(person, number)
                    .build()
            }
            SentenceType.Negative -> {
                val builder = baseNegativeBuilder()
                val particleLast = builder.getLastItem()
                val affix = pastTransitiveSuffix(particleLast)
                val affixLast = affix.last()
                val persAffix = PersAffix.getPersAffix1(person, number, affixLast, softOffset)
                builder
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    pastTransitiveCommonBuilder(person, number)
                ).build()
            }
        }
    }

    private fun ushyUshiCommonBuilder() = PhrasalBuilder()
        .verbBase(verbBase)
        .tenseAffix(Rules.USHYUSHI[softOffset])

    fun ushyUshiForm(sentenceType: SentenceType): Phrasal {
        return when (sentenceType) {
            SentenceType.Statement -> {
                ushyUshiCommonBuilder()
                    .build()
            }
            SentenceType.Negative -> {
                baseNegativeBuilder()
                    .tenseAffix(Rules.USHYUSHI[softOffset])
                    .build()
            }
            SentenceType.Question -> {
                buildQuestionForm(
                    ushyUshiCommonBuilder()
                ).build()
            }
        }

    }
}