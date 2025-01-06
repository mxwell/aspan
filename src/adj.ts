function getLastBaseReplacement(c: string): string {
    if (c == 'к') {
        return 'г';
    }
    if ( c == 'қ') {
        return 'ғ';
    }
    if (c == 'п') {
        return 'б';
    }
    return '';
}

class AdjBuilder {
    private adjDictForm: string
    private soft: boolean
    private softOffset: number

    public constructor(adjDictForm: string) {
        this.adjDictForm = adjDictForm
        this.soft = wordIsSoft(adjDictForm)
        this.softOffset = this.soft ? SOFT_OFFSET : HARD_OFFSET;
    }

    private getRakBase(last: string): string {
        const repl = getLastBaseReplacement(last);
        if (repl.length == 0) {
            return this.adjDictForm;
        }
        return replaceLast(this.adjDictForm, repl);
    }

    private getRakAffix(last: string): string {
        if (genuineVowel(last)) {
            return RAKREK[this.softOffset];
        }
        return YRAKIREK[this.softOffset];
    }

    rakForm(): Phrasal {
        const last = getLastItem(this.adjDictForm);
        const base = this.getRakBase(last);
        const affix = this.getRakAffix(last);
        return new PhrasalBuilder()
            .adjBase(base)
            .adjCompAffix(affix)
            .build();
    }
}