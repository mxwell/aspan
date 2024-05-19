function replaceBaseLastForPossessive(base: string, lastBase: string): string {
    const replacement = BASE_REPLACEMENT_PKKH.get(lastBase);
    if (replacement != null) {
        return replaceLast(base, replacement);
    } else {
        return base;
    }
}

function dropLastVowel(base: string): string {
    const n = base.length;
    return base.substring(0, n - 2) + base[n - 1];
}

class ModifiedBase {
    base: string
    endsWithVowel: boolean

    constructor(base: string, endsWithVowel: boolean) {
        this.base = base;
        this.endsWithVowel = endsWithVowel;
    }
}

class NounBuilder {
    private nounDictForm: string
    private soft: boolean
    private softOffset: number

    constructor(nounDictForm: string) {
        this.nounDictForm = nounDictForm
        this.soft = wordIsSoft(nounDictForm);
        this.softOffset = this.soft ? SOFT_OFFSET : HARD_OFFSET;
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
        let lastBase = getLastItem(this.nounDictForm);
        let pluralAffix = this.getPluralAffix(lastBase);

        return new PhrasalBuilder()
            .nounBase(this.nounDictForm)
            .pluralAffix(pluralAffix);
    }

    pluralize(): Phrasal {
        return this.pluralBuilder()
            .build();
    }

    private getDropVowelType(): DropVowelType {
        if (DROP_LAST_VOWEL_NOUNS.has(this.nounDictForm)) {
            return DropVowelType.DropLast;
        } else if (OPTIONALLY_DROP_LAST_VOWEL_NOUNS.has(this.nounDictForm)) {
            return DropVowelType.OptionallyDropLast;
        } else {
            return DropVowelType.Regular;
        }
    }

    private modifyBaseForSomePossessive(): ModifiedBase[] {
        const dropVowelType = this.getDropVowelType();
        let lastBase = getLastItem(this.nounDictForm);
        if (dropVowelType == DropVowelType.Regular) {
            const replacedStr = replaceBaseLastForPossessive(this.nounDictForm, lastBase);
            const replaced = new ModifiedBase(replacedStr, genuineVowel(getLastItem(replacedStr)));
            return [replaced];
        } else if (dropVowelType == DropVowelType.DropLast) {
            const droppedStr = dropLastVowel(this.nounDictForm);
            const dropped = new ModifiedBase(droppedStr, false);
            return [dropped];
        } else {
            const droppedStr = dropLastVowel(this.nounDictForm);
            const dropped = new ModifiedBase(droppedStr, false);
            const replacedStr = replaceBaseLastForPossessive(this.nounDictForm, lastBase);
            const replaced = new ModifiedBase(replacedStr, genuineVowel(getLastItem(replacedStr)));
            return [dropped, replaced];
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
        return new PhrasalBuilder()
            .nounBase(base.base)
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

    pluralPossessive(person: GrammarPerson, number: GrammarNumber): Phrasal {
        if (person != GrammarPerson.First) {
            return this.possessive(person, GrammarNumber.Plural);
        }
        let builder = this.pluralBuilder();
        const extraVowel = YI[this.softOffset];
        const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
        return builder
            .possessiveAffix(`${extraVowel}${affix}`)
            .build();
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

    septikForm(septik: Septik): Phrasal {
        if (septik == Septik.Shygys) {
            let lastBase = getLastItem(this.nounDictForm);
            let affix = this.getShygysAffix(lastBase, false);
            return new PhrasalBuilder()
                .nounBase(this.nounDictForm)
                .septikAffix(affix)
                .build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }

    pluralSeptikForm(septik: Septik): Phrasal {
        let builder = this.pluralBuilder();
        if (septik == Septik.Shygys) {
            const affix = DANDEN[this.softOffset];
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

        if (septik == Septik.Shygys) {
            const lastBase = getLastItem(builder.getLastItem());
            const affix = this.getShygysAffix(lastBase, person == GrammarPerson.Third);
            return builder
                .septikAffix(affix)
                .build();
        }

        return NOT_SUPPORTED_PHRASAL;
    }
}