package com.khairulin.kazakhverb.grammar

class NounBuilder(val baseBuilder: PhrasalBuilder, val soft: Boolean, val softOffset: Int) {

    companion object {
        fun ofNoun(nounDictForm: String): NounBuilder {
            val baseBuilder = PhrasalBuilder().nounBase(nounDictForm)
            val soft = Phonetics.wordIsSoft(nounDictForm.lowercase())
            val softOffset = Phonetics.softToOffset(soft)
            return NounBuilder(
                baseBuilder,
                soft,
                softOffset,
            )
        }

        fun extractLastNounPart(nounDictForm: String): String {
            var sep = nounDictForm.length - 1
            while (sep >= 0) {
                val ch = nounDictForm[sep]
                if (ch == ' ' || ch == '-') {
                    break
                }
                sep--
            }
            return if (sep < 0) nounDictForm else nounDictForm.substring(sep + 1)
        }

        fun replaceBaseLastForPossesive(baseBuilder: PhrasalBuilder, lastBase: Char): PhrasalBuilder {
            val replacement = Rules.kBaseReplacementPKKh.get(lastBase)
            if (replacement != null) {
                return baseBuilder.replaceLast(replacement)
            } else {
                return baseBuilder
            }
        }

        private fun dropLastVowelImpl(base: String): String {
            val n = base.length
            return base.substring(0, n - 2) + base[n - 1]
        }

        fun dropLastVowel(baseBuilder: PhrasalBuilder): PhrasalBuilder {
            val lastPart = baseBuilder.getLastPart()
            val modified = dropLastVowelImpl(lastPart.content)
            val modifiedPart = lastPart.copy(modified)
            return baseBuilder.replaceLastPart(modifiedPart)
        }
    }

    private fun copyBase(): PhrasalBuilder {
        return baseBuilder.copy()
    }

    private fun getPluralAffix(baseLast: Char): String {
        return AffixHelpers.chooseLDT(
            baseLast,
            this.softOffset,
            Rules.LARLER,
            Rules.DARDER,
            Rules.TARTER
        )
    }

    private fun pluralBuilder(): PhrasalBuilder {
        val lastBase = baseBuilder.getLastItem()
        val pluralAffix = getPluralAffix(lastBase)

        return this.copyBase()
            .pluralAffix(pluralAffix)
    }

    fun pluralize(): Phrasal {
        return pluralBuilder().build()
    }

    private fun getDropVowelType(): DropVowelType {
        val rawBase = baseBuilder.getFirstPart().content
        val lastPart = extractLastNounPart(rawBase).lowercase()
        if (Rules.kDropLastVowelNouns.contains(lastPart)) {
            return DropVowelType.DropLast
        } else if (Rules.kOptionallyDropLastVowelNouns.contains(lastPart)) {
            return DropVowelType.OptionallyDropLast
        } else {
            return DropVowelType.Regular
        }
    }

    private fun modifyBaseWithDrop(): ModifiedBase {
        val builderWithDrop = dropLastVowel(this.copyBase())
        return ModifiedBase(
            builderWithDrop,
            false
        )
    }

    private fun modifyBaseWithReplacement(): ModifiedBase {
        val lastBase = baseBuilder.getLastItem()
        val builderWithReplacement = replaceBaseLastForPossesive(
            copyBase(),
            lastBase
        )
        return ModifiedBase(
            builderWithReplacement,
            Phonetics.genuineVowel(builderWithReplacement.getLastItem())
        )
    }

    private fun modifyBaseForPossessive(): List<ModifiedBase> {
        val dropVowelType = getDropVowelType()
        if (dropVowelType == DropVowelType.Regular) {
            return listOf(modifyBaseWithReplacement())
        } else if (dropVowelType == DropVowelType.DropLast) {
            return listOf(modifyBaseWithDrop())
        } else {
            return listOf(
                modifyBaseWithDrop(),
                modifyBaseWithReplacement(),
            )
        }
    }

    private fun singlePossessiveBuilder(base: ModifiedBase, person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        val extra = if (person == GrammarPerson.Third) {
            if (base.endsWithVowel) {
                "с"
            } else {
                ""
            }
        } else if (!base.endsWithVowel) {
            Rules.YI[softOffset]
        } else {
            ""
        }
        val affix = Rules.NOUN_POSSESSIVE_AFFIXES[person]!![number]!![softOffset]
        return base.base.copy()
            .possessiveAffix("${extra}${affix}")
    }

    private fun buildPossessiveWithAlternative(bases: List<ModifiedBase>, person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        var mainBuilder = singlePossessiveBuilder(bases[0], person, number)
        if (bases.size > 1) {
            val alternative = singlePossessiveBuilder(bases[1], person, number)
            mainBuilder = mainBuilder.addAlternative(alternative)
        }
        return mainBuilder
    }

    private fun possessiveBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (person == GrammarPerson.First) {
            val bases = modifyBaseForPossessive()
            return buildPossessiveWithAlternative(bases, person, number)
        } else if (person == GrammarPerson.Second || person == GrammarPerson.SecondPolite) {
            if (number == GrammarNumber.Singular) {
                val bases = modifyBaseForPossessive()
                return buildPossessiveWithAlternative(bases, person, number)
            } else {
                val baseWithNumber = pluralBuilder()
                val extraVowel = Rules.YI[softOffset]
                val affix = Rules.NOUN_POSSESSIVE_AFFIXES[person]!![number]!![softOffset]
                return baseWithNumber.possessiveAffix("${extraVowel}${affix}")
            }
        } else if (person == GrammarPerson.Third) {
            if (number == GrammarNumber.Singular) {
                val bases = modifyBaseForPossessive()
                return buildPossessiveWithAlternative(bases, person, number)
            } else {
                val baseWithNumber = pluralBuilder()
                val affix = Rules.NOUN_POSSESSIVE_AFFIXES[person]!![number]!![softOffset]
                return baseWithNumber
                    .possessiveAffix(affix)
            }
        }
        return PhrasalBuilder()
    }

    private fun getTabysAffix(last: Char, thirdPersonPoss: Boolean): String {
        if (thirdPersonPoss) {
            return "н"
        } else if (Rules.VOWELS_GROUP1.contains(last) || Rules.CONS_GROUP1_2.contains(last)) {
            return Rules.DYDI[softOffset]
        } else if (Phonetics.isVowel(last)) {
            return Rules.NYNI[softOffset]
        } else {
            return Rules.TYTI[softOffset]
        }
    }

    fun septikForm(septik: Septik): Phrasal {
        if (septik == Septik.Atau) {
            return copyBase().build()
        }

        val lastBase = baseBuilder.getLastItem()

        if (septik == Septik.Tabys) {
            val affix = getTabysAffix(lastBase, false)
            return copyBase()
                .septikAffix(affix)
                .build()
        }

        return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
    }

    fun pluralSeptikForm(septik: Septik): Phrasal {
        val builder = pluralBuilder()

        if (septik == Septik.Tabys) {
            val affix = Rules.DYDI[softOffset]
            return builder
                .septikAffix(affix)
                .build()
        }

        return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
    }

    fun possessiveSeptikForm(person: GrammarPerson, number: GrammarNumber, septik: Septik): Phrasal {
        val builder = possessiveBuilder(person, number)

        if (builder.isEmpty) {
            return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }

        if (septik == Septik.Atau) {
            return builder
                .build()
        }

        val lastBase = builder.getLastItem()

        if (septik == Septik.Tabys) {
            val affix = getTabysAffix(lastBase, person == GrammarPerson.Third)
            return builder
                .septikAffix(affix)
                .build()
        }

        return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
    }
}