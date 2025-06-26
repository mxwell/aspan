package com.khairulin.kazakhverb.grammar

class NounBuilder(val baseBuilder: PhrasalBuilder, val softOffset: Int) {

    companion object {
        fun ofNoun(nounDictForm: String): NounBuilder {
            val baseBuilder = PhrasalBuilder().nounBase(nounDictForm)
            val soft = Phonetics.wordIsSoft(nounDictForm.lowercase())
            val softOffset = Phonetics.softToOffset(soft)
            return NounBuilder(
                baseBuilder,
                softOffset,
            )
        }

        fun ofPhrasalBuilder(baseBuilder: PhrasalBuilder, softOffset: Int): NounBuilder {
            return NounBuilder(
                baseBuilder,
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

    private fun getShygysAffix(last: Char, thirdPersonPoss: Boolean): String {
        if (thirdPersonPoss || Rules.CONS_GROUP6.contains(last)) {
            return Rules.NANNEN[softOffset]
        } else if (Phonetics.isVowel(last) || Rules.CONS_GROUP1_3.contains(last)) {
            return Rules.DANDEN[softOffset]
        } else {
            return Rules.TANTEN[softOffset]
        }
    }

    private fun getJatysAffix(last: Char, thirdPersonPoss: Boolean): String {
        if (thirdPersonPoss) {
            return Rules.NDANDE[softOffset]
        } else if (Phonetics.isVowel(last) || Rules.CONS_GROUP1_2.contains(last)) {
            return Rules.DADE[softOffset]
        } else {
            return Rules.TATE[softOffset]
        }
    }

    private fun getBarysAffix(last: Char, person: GrammarPerson?, number: GrammarNumber?): String {
        if ((person == GrammarPerson.First && number == GrammarNumber.Singular) || person == GrammarPerson.Second) {
            return Rules.AE[softOffset]
        } else if (person == GrammarPerson.Third) {
            return Rules.NANE[softOffset]
        } else {
            if (Phonetics.isVowel(last) || Rules.CONS_GROUP1_2.contains(last)) {
                return Rules.GAGE[softOffset]
            } else {
                return Rules.KAKE[softOffset]
            }
        }
    }

    private fun getIlikAffix(last: Char, thirdPersonPoss: Boolean): String {
        if (Rules.VOWELS_GROUP1.contains(last) || Rules.CONS_GROUP1_3.contains(last)) {
            return Rules.DYNGDING[softOffset]
        } else if (Phonetics.isVowel(last) || Rules.CONS_GROUP6.contains(last) || thirdPersonPoss) {
            return Rules.NYNGNING[softOffset]
        } else {
            return Rules.TYNGTING[softOffset]
        }
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

    private fun getKomektesAffix(last: Char, thirdPersonPoss: Boolean): String {
        if (thirdPersonPoss || Phonetics.isVowel(last) || Rules.CONS_GROUP1_6.contains(last)) {
            return "мен"
        } else if (Rules.CONS_GROUP3.contains(last)) {
            return "бен"
        } else {
            return "пен"
        }
    }

    fun septikForm(septik: Septik): Phrasal {
        val lastBase = baseBuilder.getLastItem()

        val affix = when (septik) {
            Septik.Atau -> {
                return copyBase().build()
            }
            Septik.Ilik -> getIlikAffix(lastBase, false)
            Septik.Barys -> getBarysAffix(lastBase, null, null)
            Septik.Tabys -> getTabysAffix(lastBase, false)
            Septik.Jatys -> getJatysAffix(lastBase, false)
            Septik.Shygys -> getShygysAffix(lastBase, false)
            Septik.Komektes -> getKomektesAffix(lastBase, false)
        }
        return copyBase()
            .septikAffix(affix)
            .build()
    }

    fun pluralSeptikForm(septik: Septik): Phrasal {
        val builder = pluralBuilder()

        val affix = when (septik) {
            Septik.Atau -> {
                return builder.build()
            }
            Septik.Ilik -> Rules.DYNGDING[softOffset]
            Septik.Barys -> Rules.GAGE[softOffset]
            Septik.Tabys -> Rules.DYDI[softOffset]
            Septik.Jatys -> Rules.DADE[softOffset]
            Septik.Shygys -> Rules.DANDEN[softOffset]
            Septik.Komektes -> "мен"
        }

        return builder
            .septikAffix(affix)
            .build()
    }

    fun possessiveSeptikForm(person: GrammarPerson, number: GrammarNumber, septik: Septik): Phrasal {
        val builder = possessiveBuilder(person, number)

        if (builder.isEmpty) {
            return PhrasalBuilder.NOT_SUPPORTED_PHRASAL
        }

        val lastBase = builder.getLastItem()
        val thirdPerson = person == GrammarPerson.Third

        val affix = when (septik) {
            Septik.Atau -> {
                return builder.build()
            }
            Septik.Ilik -> getIlikAffix(lastBase, thirdPerson)
            Septik.Barys -> getBarysAffix(lastBase, person, number)
            Septik.Tabys -> getTabysAffix(lastBase, thirdPerson)
            Septik.Jatys -> getJatysAffix(lastBase, thirdPerson)
            Septik.Shygys -> getShygysAffix(lastBase, thirdPerson)
            Septik.Komektes -> getKomektesAffix(lastBase, thirdPerson)
        }
        return builder
            .septikAffix(affix)
            .build()
    }
}