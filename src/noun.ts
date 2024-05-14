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

    pluralize(): Phrasal {
        let lastBase = getLastItem(this.nounDictForm);
        let pluralAffix = this.getPluralAffix(lastBase);

        return new PhrasalBuilder()
            .nounBase(this.nounDictForm)
            .pluralAffix(pluralAffix)
            .build();
    }

    possessive(person: GrammarPerson, number: GrammarNumber): Phrasal {
        let lastBase = getLastItem(this.nounDictForm);
        const isVowel = genuineVowel(lastBase);
        if (person == GrammarPerson.First) {
            const replacement = BASE_REPLACEMENT_PKKH.get(lastBase);
            const base = (
                (replacement != null)
                ? replaceLast(this.nounDictForm, replacement)
                : this.nounDictForm
            );
            const extraVowel = isVowel ? "" : YI[this.softOffset];
            const affix = NOUN_POSSESSIVE_AFFIXES[person][number][this.softOffset];
            return new PhrasalBuilder()
                .nounBase(base)
                .possessiveAffix(`${extraVowel}${affix}`)
                .build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
}