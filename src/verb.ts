function validateVerb(verb_dict_form: string): boolean {
    if (verb_dict_form.length < 2) {
        return false;
    }
    if (verb_dict_form.length > 100) {
        return false;
    }
    if (verb_dict_form.toLowerCase() != verb_dict_form) {
        return false;
    }
    let last = getLastItem(verb_dict_form);
    return last == "у" || last == "ю";
}

function isVerbException(verb_dict_form: string): boolean {
    return VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET.has(verb_dict_form);
}

function isVerbOptionalException(verb_dict_form): boolean {
    return VERB_PRESENT_TRANSITIVE_OPTIONAL_EXCEPTIONS_SET.has(verb_dict_form);
}

function isVerbException2(verb_dict_form: string): boolean {
    return VERB_PRESENT_TRANSITIVE_EXCEPTIONS2_SET.has(verb_dict_form);
}

const NOT_SUPPORTED: string = "<not supported>";

class PresentContinuousContext {
    verb_base: string
    constructor(verb_dict_form: string) {
        this.verb_base = VERB_PRESENT_CONT_BASE_MAP.get(verb_dict_form);
    }
}

type MaybePresentContinuousContext = PresentContinuousContext | null;

function createPresentContinuousContext(verb_dict_form: string): MaybePresentContinuousContext {
    if (VERB_PRESENT_CONT_BASE_MAP.has(verb_dict_form)) {
        return new PresentContinuousContext(verb_dict_form);
    }
    return null;
}

class VerbBuilder {
    verb_dict_form: string
    verb_base: string
    needs_ya_suffix: boolean
    base_last: string
    soft: boolean
    soft_offset: number
    cont_context: MaybePresentContinuousContext
    constructor(verb_dict_form: string, force_exceptional = false) {
        if (!validateVerb(verb_dict_form)) {
            throw new Error("verb dictionary form must end with -у/-ю");
        }
        this.verb_dict_form = verb_dict_form;
        this.verb_base = verb_dict_form.slice(0, verb_dict_form.length - 1);
        this.needs_ya_suffix = false;
        this.soft = (
            wordIsSoft(this.verb_base)
            || FORCED_SOFT_VERBS.has(verb_dict_form)
        );
        this.soft_offset = this.soft ? 1 : 0;

        /* exceptions */
        if (isVerbException(verb_dict_form) || (isVerbOptionalException(verb_dict_form) && force_exceptional)) {
            this.verb_base = this.verb_base + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.soft_offset];
        } else if (isVerbException2(verb_dict_form)) {
            this.verb_base = this.verb_base + "й" + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.soft_offset];
        } else if (verb_dict_form.endsWith("ю")) {
            if (verb_dict_form.endsWith("ию")) {
                if (!this.soft) {
                    this.needs_ya_suffix = true;
                } else {
                    // nothing special here
                }
            } else {
                this.verb_base = this.verb_base + "й";
            }
        }

        this.base_last = getLastItem(this.verb_base);

        this.cont_context = createPresentContinuousContext(verb_dict_form);
    }
    getPersAffix1ExceptThirdPerson(person: GrammarPerson, number: GrammarNumber): string {
        if (person == "Third") {
            return "";
        }
        return VERB_PERS_AFFIXES1[person][number][this.soft_offset];
    }
    /* used only for Statement/Question sentence types */
    presentTransitiveSuffix(): string {
        if (this.needs_ya_suffix) {
            return "я";
        }
        if (genuineVowel(this.base_last)) {
            return "й";
        }
        if (this.soft) {
            return "е";
        }
        return "а";
    }
    /* Ауыспалы осы/келер шақ */
    presentTransitiveForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (sentenceType == "Statement") {
            let affix = this.presentTransitiveSuffix();
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.soft_offset];
            return fixShortIBigrams(`${this.verb_base}${affix}${persAffix}`);
        } else if (sentenceType == "Negative") {
            let particle = getQuestionParticle(this.base_last, this.soft_offset);
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.soft_offset];
            return fixBgBigrams(`${this.verb_base}${particle}й${persAffix}`);
        } else if (sentenceType == "Question") {
            let affix = this.presentTransitiveSuffix();
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            let verb = `${this.verb_base}${affix}${persAffix}`;
            let particle = getQuestionParticle(getLastItem(verb), this.soft_offset);
            return fixShortIBigrams(`${verb} ${particle}?`);
        }
        return NOT_SUPPORTED;
    }
    /* Нақ осы шақ */
    presentSimpleContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (this.cont_context == null) {
            return NOT_SUPPORTED;
        }
        if (sentenceType == "Statement") {
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            return `${this.cont_context.verb_base}${persAffix}`;
        }
        return NOT_SUPPORTED;
    }
}