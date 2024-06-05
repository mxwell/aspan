function extractLastNounPart(nounDictForm: string): string {
    let sep = nounDictForm.length - 1;
    while (sep >= 0) {
        const ch = nounDictForm[sep];
        if (ch == ' ' || ch == '-') {
            break;
        }
        --sep;
    }
    if (sep < 0) {
        return nounDictForm;
    }
    return nounDictForm.substring(sep + 1);
}

class DeclensionAltInfo {
    noun: string
    dropVowelMeaning: string
    keepVowelMeaning: string
    constructor(noun: string, dropVowelMeaning: string, keepVowelMeaning: string) {
        this.noun = noun;
        this.dropVowelMeaning = dropVowelMeaning;
        this.keepVowelMeaning = keepVowelMeaning;
    }
}

type MaybeDeclensionAltInfo = DeclensionAltInfo | null;

function getDeclAltInfo(nounDictForm: string): MaybeDeclensionAltInfo {
    const lastPart = extractLastNounPart(nounDictForm).toLowerCase();
    if (OPTIONALLY_DROP_LAST_VOWEL_NOUNS.has(lastPart)) {
        const meanings = OPTIONALLY_DROP_LAST_VOWEL_NOUNS.get(lastPart);
        if (meanings.length == 2) {
            return new DeclensionAltInfo(
                lastPart,
                meanings[1],
                meanings[0]
            );
        }
    }
    return null;
}

function replaceBaseLastForPossessive(baseBuilder: PhrasalBuilder, lastBase: string): PhrasalBuilder {
    const replacement = BASE_REPLACEMENT_PKKH.get(lastBase);
    if (replacement != null) {
        return baseBuilder.replaceLast(replacement);
    } else {
        return baseBuilder;
    }
}

function dropLastVowelImpl(base: string): string {
    const n = base.length;
    return base.substring(0, n - 2) + base[n - 1];
}

function dropLastVowel(baseBuilder: PhrasalBuilder): PhrasalBuilder {
    const lastPart = baseBuilder.getLastPart();
    const modified = dropLastVowelImpl(lastPart.content);
    const modifiedPart = lastPart.copy(modified);
    return baseBuilder.replaceLastPart(modifiedPart);
}

class ModifiedBase {
    base: PhrasalBuilder
    endsWithVowel: boolean

    constructor(base: PhrasalBuilder, endsWithVowel: boolean) {
        this.base = base;
        this.endsWithVowel = endsWithVowel;
    }
}

type MaybeGrammarPerson = GrammarPerson | null;
type MaybeGrammarNumber = GrammarNumber | null;

class NounBuilder {
    private baseBuilder: PhrasalBuilder
    private soft: boolean
    private softOffset: number

    public constructor(nounDictForm: string);
    public constructor(baseBuilder: PhrasalBuilder, soft: boolean);
    public constructor(...arr: any[]) {
        if (arr.length == 1) {
            if (typeof arr[0] === 'string') {
                const nounDictForm = arr[0];
                this.baseBuilder = new PhrasalBuilder().nounBase(nounDictForm);
                this.soft = wordIsSoft(nounDictForm);
                this.softOffset = this.soft ? SOFT_OFFSET : HARD_OFFSET;
            } else {
                throw new Error("Invalid single arguments");
            }
        } else if (arr.length == 2) {
            if (arr[0] instanceof PhrasalBuilder && typeof arr[1] === 'boolean') {
                this.baseBuilder = arr[0];
                this.soft = arr[1];
                this.softOffset = this.soft ? SOFT_OFFSET : HARD_OFFSET;
            } else {
                throw new Error("Invalid pair of arguments");
            }
        } else {
            throw new Error("Invalid number of arguments");
        }
    }

    private copyBase(): PhrasalBuilder {
        return this.baseBuilder.copy();
    }

    private getPluralAffix(baseLast): string {
        if (vowel(baseLast) || checkCharPresence(baseLast, CONS_GROUP7)) {
            return LARLER[this.softOffset];
        }
        if (checkCharPresence(baseLast, CONS_GROUP8)) {
            return DARDER[this.softOffset];
        }
        return TARTER[this.softOffset];
    }

    private pluralBuilder(): PhrasalBuilder {
        let lastBase = this.baseBuilder.getLastItem();
        let pluralAffix = this.getPluralAffix(lastBase);

        return this.copyBase()
            .pluralAffix(pluralAffix);
    }

    pluralize(): Phrasal {
        return this.pluralBuilder()
            .build();
    }

    private getDropVowelType(): DropVowelType {
        const rawBase = this.baseBuilder.getFirstPart().content;
        const lastPart = extractLastNounPart(rawBase).toLowerCase();
        if (DROP_LAST_VOWEL_NOUNS.has(lastPart)) {
            return DropVowelType.DropLast;
        } else if (OPTIONALLY_DROP_LAST_VOWEL_NOUNS.has(lastPart)) {
            return DropVowelType.OptionallyDropLast;
        } else {
            return DropVowelType.Regular;
        }
    }

    private modifyBaseForSomePossessive(): ModifiedBase[] {
        const dropVowelType = this.getDropVowelType();
        let lastBase = this.baseBuilder.getLastItem();
        if (dropVowelType == DropVowelType.Regular) {
            let builderWithReplacement = replaceBaseLastForPossessive(this.copyBase(), lastBase);
            const modifiedBase = new ModifiedBase(builderWithReplacement, genuineVowel(builderWithReplacement.getLastItem()));
            return [modifiedBase];
        } else if (dropVowelType == DropVowelType.DropLast) {
            let builderWithDrop = dropLastVowel(this.copyBase());
            const modifiedBase = new ModifiedBase(builderWithDrop, false);
            return [modifiedBase];
        } else {
            let builderWithDrop = dropLastVowel(this.copyBase());
            const modifiedWithDrop = new ModifiedBase(builderWithDrop, false);
            const builderWithReplacement = replaceBaseLastForPossessive(this.copyBase(), lastBase);
            const modifiedWithReplacement = new ModifiedBase(builderWithReplacement, genuineVowel(builderWithReplacement.getLastItem()));
            return [modifiedWithDrop, modifiedWithReplacement];
        }
    }

    private singlePossessiveBuilder(base: ModifiedBase, person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        let extra = "";
        if (person == GrammarPerson.Third) {
            if (base.endsWithVowel) {
                extra = "с";
            }
        } else {
            if (!base.endsWithVowel) {
                extra = YI[this.softOffset];
            }
        }
        const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
        return base.base.copy()
            .possessiveAffix(`${extra}${affix}`);
    }

    private buildPossessiveWithAlternative(bases: ModifiedBase[], person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        let mainBuilder = this.singlePossessiveBuilder(bases[0], person, number);
        if (bases.length > 1) {
            const alternative = this.singlePossessiveBuilder(bases[1], person, number);
            mainBuilder = mainBuilder.addAlternative(alternative);
        }
        return mainBuilder;
    }

    private possessiveBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (person == GrammarPerson.First) {
            const bases = this.modifyBaseForSomePossessive();
            return this.buildPossessiveWithAlternative(bases, person, number);
        } else if (person == GrammarPerson.Second || person == GrammarPerson.SecondPolite) {
            if (number == GrammarNumber.Singular) {
                const bases = this.modifyBaseForSomePossessive();
                return this.buildPossessiveWithAlternative(bases, person, number);
            } else {
                const baseWithNumber = this.pluralBuilder();
                const extraVowel = YI[this.softOffset];
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return baseWithNumber
                    .possessiveAffix(`${extraVowel}${affix}`);
            }
        } else if (person == GrammarPerson.Third) {
            if (number == GrammarNumber.Singular) {
                const bases = this.modifyBaseForSomePossessive();
                return this.buildPossessiveWithAlternative(bases, person, number);
            } else {
                const baseWithNumber = this.pluralBuilder();
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return baseWithNumber
                    .possessiveAffix(affix);
            }
        }
        return new PhrasalBuilder();
    }

    possessive(person: GrammarPerson, number: GrammarNumber): Phrasal {
        let builder = this.possessiveBuilder(person, number);
        if (builder.isEmpty()) {
            return NOT_SUPPORTED_PHRASAL;
        }
        return builder.build();
    }

    private pluralPossessiveBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (person != GrammarPerson.First) {
            return this.possessiveBuilder(person, GrammarNumber.Plural);
        }
        let builder = this.pluralBuilder();
        const extraVowel = YI[this.softOffset];
        const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
        return builder
            .possessiveAffix(`${extraVowel}${affix}`);
    }

    pluralPossessive(person: GrammarPerson, number: GrammarNumber): Phrasal {
        let builder = this.pluralPossessiveBuilder(person, number);
        if (builder.isEmpty()) {
            return NOT_SUPPORTED_PHRASAL;
        }
        return builder.build();
    }

    private getShygysAffix(last: string, thirdPersonPoss: boolean): string {
        if (thirdPersonPoss || checkCharPresence(last, CONS_GROUP6)) {
            return NANNEN[this.softOffset];
        } else if (vowel(last) || checkCharPresence(last, CONS_GROUP1_3)) {
            return DANDEN[this.softOffset];
        } else {
            return TANTEN[this.softOffset];
        }
    }

    private getJatysAffix(last: string, thirdPersonPoss: boolean): string {
        if (thirdPersonPoss) {
            return NDANDE[this.softOffset];
        } else if (vowel(last) || checkCharPresence(last, CONS_GROUP1_2)) {
            return DADE[this.softOffset];
        } else {
            return TATE[this.softOffset];
        }
    }

    private getRelatedAdjAffix(last: string, thirdPersonPoss: boolean): string {
        if (thirdPersonPoss) {
            return NDAGYNDEGI[this.softOffset];
        } else if (vowel(last) || checkCharPresence(last, CONS_GROUP1_2)) {
            return DAGYDEGI[this.softOffset];
        } else {
            return TAGYTEGI[this.softOffset];
        }
    }

    private getBarysAffix(last: string, person: MaybeGrammarPerson, number: MaybeGrammarNumber): string {
        if ((person == GrammarPerson.First && number == GrammarNumber.Singular) || person == GrammarPerson.Second) {
            return AE[this.softOffset];
        } else if (person == GrammarPerson.Third) {
            return NANE[this.softOffset];
        } else {
            if (vowel(last) || checkCharPresence(last, CONS_GROUP1_2)) {
                return GAGE[this.softOffset];
            } else {
                return KAKE[this.softOffset];
            }
        }
    }

    private getIlikAffix(last: string, thirdPersonPoss: boolean): string {
        if (checkCharPresence(last, VOWELS_GROUP1) || checkCharPresence(last, CONS_GROUP1_3)) {
            return DYNGDING[this.softOffset];
        } else if (vowel(last) || checkCharPresence(last, CONS_GROUP6) || thirdPersonPoss) {
            return NYNGNING[this.softOffset];
        } else {
            return TYNGTING[this.softOffset];
        }
    }

    private getTabysAffix(last: string, thirdPersonPoss: boolean): string {
        if (thirdPersonPoss) {
            return "н";
        } else if (checkCharPresence(last, VOWELS_GROUP1) || checkCharPresence(last, CONS_GROUP1_2)) {
            return DYDI[this.softOffset];
        } else if (vowel(last)) {
            return NYNI[this.softOffset];
        } else {
            return TYTI[this.softOffset];
        }
    }

    private getKomektesAffix(last: string, thirdPersonPoss: boolean): string {
        if (thirdPersonPoss || vowel(last) || checkCharPresence(last, CONS_GROUP1_6)) {
            return "мен";
        } else if (checkCharPresence(last, CONS_GROUP3)) {
            return "бен";
        } else {
            return "пен";
        }
    }

    septikForm(septik: Septik): Phrasal {
        if (septik == Septik.Atau) {
            return this.copyBase()
                .build();
        }

        const lastBase = this.baseBuilder.getLastItem();

        if (septik == Septik.Shygys) {
            let affix = this.getShygysAffix(lastBase, false);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Jatys) {
            let affix = this.getJatysAffix(lastBase, false);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Barys) {
            let affix = this.getBarysAffix(lastBase, null, null);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Ilik) {
            let affix = this.getIlikAffix(lastBase, false);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Tabys) {
            let affix = this.getTabysAffix(lastBase, false);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Komektes) {
            let affix = this.getKomektesAffix(lastBase, false);
            return this.copyBase()
                .septikAffix(affix)
                .build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }

    pluralSeptikForm(septik: Septik): Phrasal {
        let builder = this.pluralBuilder();
        if (septik == Septik.Atau) {
            return builder
                .build();
        } else if (septik == Septik.Shygys) {
            const affix = DANDEN[this.softOffset];
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Jatys) {
            const affix = DADE[this.softOffset];
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Barys) {
            const affix = GAGE[this.softOffset];
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Ilik) {
            const affix = DYNGDING[this.softOffset];
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Tabys) {
            const affix = DYDI[this.softOffset];
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Komektes) {
            const affix = "мен";
            return builder
                .septikAffix(affix)
                .build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }

    possessiveSeptikForm(person: GrammarPerson, number: GrammarNumber, septik: Septik): Phrasal {
        let builder = this.possessiveBuilder(person, number);
        if (builder.isEmpty()) {
            return NOT_SUPPORTED_PHRASAL;
        }

        if (septik == Septik.Atau) {
            return builder
                .build();
        }

        const lastBase = builder.getLastItem();

        if (septik == Septik.Shygys) {
            const affix = this.getShygysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Jatys) {
            const affix = this.getJatysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Barys) {
            const affix = this.getBarysAffix(lastBase, person, number);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Ilik) {
            const affix = this.getIlikAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Tabys) {
            const affix = this.getTabysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Komektes) {
            const affix = this.getKomektesAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        }

        return NOT_SUPPORTED_PHRASAL;
    }

    pluralPossessiveSeptikForm(person: GrammarPerson, number: GrammarNumber, septik: Septik): Phrasal {
        let builder = this.pluralPossessiveBuilder(person, number);
        if (builder.isEmpty()) {
            return NOT_SUPPORTED_PHRASAL;
        }

        if (septik == Septik.Atau) {
            return builder
                .build();
        }

        const lastBase = builder.getLastItem();

        if (septik == Septik.Shygys) {
            const affix = this.getShygysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Jatys) {
            const affix = this.getJatysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Barys) {
            const affix = this.getBarysAffix(lastBase, person, number);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Ilik) {
            const affix = this.getIlikAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Tabys) {
            const affix = this.getTabysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        } else if (septik == Septik.Komektes) {
            const affix = this.getKomektesAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        }

        return NOT_SUPPORTED_PHRASAL;
    }

    private getSpecialPossessiveAffix(last: string): string {
        if (genuineVowel(last)) {
            return "нікі";
        } else if (checkCharPresence(last, CONS_GROUP1_2)) {
            return "дікі";
        } else {
            return "тікі";
        }
    }

    specialPossessive(): Phrasal {
        const lastBase = this.baseBuilder.getLastItem();
        let affix = this.getSpecialPossessiveAffix(lastBase);
        return this.copyBase()
            .possessiveAffix(affix)
            .build();
    }

    pluralSpecialPossessive(): Phrasal {
        let builder = this.pluralBuilder();
        const affix = "дікі";
        return builder
            .possessiveAffix(affix)
            .build();
    }

    relatedAdj(): Phrasal {
        const lastBase = this.baseBuilder.getLastItem();
        let affix = this.getRelatedAdjAffix(lastBase, false);
        return this.copyBase()
            .septikAffix(affix)
            .build();
    }

    possessiveRelatedAdj(person: GrammarPerson, number: GrammarNumber): Phrasal {
        let builder = this.possessiveBuilder(person, number);
        if (builder.isEmpty()) {
            return NOT_SUPPORTED_PHRASAL;
        }
        const lastBase = builder.getLastItem();
        const affix = this.getRelatedAdjAffix(lastBase, person == GrammarPerson.Third);
        return builder
            .septikAffix(affix)
            .build();
    }
}