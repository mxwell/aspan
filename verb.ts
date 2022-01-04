class VerbBuilder {
    verb_dict_form: string
    verb_base: string
    base_last: string
    soft: boolean
    soft_offset: number
    constructor(verb_dict_form: string) {
        let last = getLastItem(verb_dict_form);
        if (!(last == "у" || last == "ю")) {
            throw new Error("verb must end with either 'у' or 'ю'");
        }
        this.verb_dict_form = verb_dict_form;
        this.verb_base = verb_dict_form.slice(0, verb_dict_form.length - 1);
        this.base_last = getLastItem(this.verb_base);
        this.soft = word_is_soft(this.verb_base);
        this.soft_offset = this.soft ? 1 : 0;
    }
    present_transitive_suffix(): string {
        if (check_string_in_list(this.verb_dict_form, PRESENT_TRANSITIVE_EXCEPT_VERBS1)) {
            return "и";
        }
        if (check_string_in_list(this.verb_dict_form, PRESENT_TRANSITIVE_EXCEPT_VERBS2)) {
            return "я";
        }
        if (check_string_in_list(this.verb_dict_form, PRESENT_TRANSITIVE_EXCEPT_VERBS3)) {
            return "йе";
        }
        if (vowel(this.base_last)) {
            return "й";
        }
        if (this.soft) {
            return "е";
        }
        return "а";
    }

    present_transitive_form(face: Face, plurality: Plurality, sentence_type: SentenceType): string {
        if (sentence_type == "Statement") {
            let affix = this.present_transitive_suffix();
            let pers_affix = PRESENT_TRANSITIVE_AFFIXES[face][plurality][this.soft_offset];
            return `${this.verb_base}${affix}${pers_affix}`;
        } else if (sentence_type == "Negative") {
            let particle = getQuestionParticle(this.base_last, this.soft_offset);
            let pers_affix = PRESENT_TRANSITIVE_AFFIXES[face][plurality][this.soft_offset];
            return `${this.verb_base}${particle}й${pers_affix}`
        } else if (sentence_type == "Question") {
            let verb: string;
            let affix = this.present_transitive_suffix();
            if (face == "Third") {
                verb = `${this.verb_base}${affix}`;
            } else {
                let pers_affix = PRESENT_TRANSITIVE_AFFIXES[face][plurality][this.soft_offset];
                verb = `${this.verb_base}${affix}${pers_affix}`;
            }
            let particle = getQuestionParticle(getLastItem(verb), this.soft_offset);
            return `${verb} ${particle}?`;
        }
        return "unsupported";
    }
}