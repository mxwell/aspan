function replaceBaseLastForPossessive(base: string, lastBase: string): string {
    const replacement = BASE_REPLACEMENT_PKKH.get(lastBase);
    if (replacement != null) {
        return replaceLast(base, replacement);
    } else {
        return base;
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

    possessive(person: GrammarPerson, number: GrammarNumber): Phrasal {
        let lastBase = getLastItem(this.nounDictForm);
        const isVowel = genuineVowel(lastBase);
        if (person == GrammarPerson.First) {
            const base = replaceBaseLastForPossessive(this.nounDictForm, lastBase);
            const extraVowel = isVowel ? "" : YI[this.softOffset];
            const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
            return new PhrasalBuilder()
                .nounBase(base)
                .possessiveAffix(`${extraVowel}${affix}`)
                .build();
        } else if (person == GrammarPerson.Second || person == GrammarPerson.SecondPolite) {
            if (number == GrammarNumber.Singular) {
                const base = replaceBaseLastForPossessive(this.nounDictForm, lastBase);
                const extraVowel = isVowel ? "" : YI[this.softOffset];
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return new PhrasalBuilder()
                    .nounBase(base)
                    .possessiveAffix(`${extraVowel}${affix}`)
                    .build();
            } else {
                const baseWithNumber = this.pluralBuilder();
                const extraVowel = YI[this.softOffset];
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return baseWithNumber
                    .possessiveAffix(`${extraVowel}${affix}`)
                    .build();
            }
        } else if (person == GrammarPerson.Third) {
            if (number == GrammarNumber.Singular) {
                const base = replaceBaseLastForPossessive(this.nounDictForm, lastBase);
                const extraConsonant = isVowel ? "с" : "";
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return new PhrasalBuilder()
                    .nounBase(base)
                    .possessiveAffix(`${extraConsonant}${affix}`)
                    .build();
            } else {
                const baseWithNumber = this.pluralBuilder();
                const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
                return baseWithNumber
                    .possessiveAffix(affix)
                    .build();
            }
        }
        return NOT_SUPPORTED_PHRASAL;
    }
}