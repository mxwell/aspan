function validateVerb(verbDictForm: string): boolean {
    if (verbDictForm.length < 2) {
        return false;
    }
    if (verbDictForm.length > 100) {
        return false;
    }
    if (verbDictForm.toLowerCase() != verbDictForm) {
        return false;
    }
    let last = getLastItem(verbDictForm);
    return last == "у" || last == "ю";
}

function isVerbException(verbDictForm: string): boolean {
    return VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET.has(verbDictForm);
}

function isVerbOptionalException(verbDictForm: string): boolean {
    return VERB_PRESENT_TRANSITIVE_OPTIONAL_EXCEPTIONS_SET.has(verbDictForm);
}

function isVerbException2(verbDictForm: string): boolean {
    return VERB_PRESENT_TRANSITIVE_EXCEPTIONS2_SET.has(verbDictForm);
}

const NOT_SUPPORTED: string = "<not supported>";

class PresentContinuousContext {
    verbBase: string
    constructor(verbDictFormBase: string) {
        this.verbBase = verbDictFormBase;
    }
}

function validPresentContAuxVerb(verbDictForm: string): boolean {
    return VERB_PRESENT_CONT_BASE_MAP.has(verbDictForm);
}

function validPresentContPair(verb: string, auxVerb: string): boolean {
    if (!validPresentContAuxVerb(auxVerb)) {
        return false;
    }
    const aeException = VERB_PRESENT_CONT_EXCEPTION_A_SET.has(verb) || VERB_PRESENT_CONT_EXCEPTION_E_SET.has(verb);
    if (aeException && auxVerb != VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED) {
        return false;
    }
    return true;
}

type MaybePresentContinuousContext = PresentContinuousContext | null;
type MaybeVerbBuilder = VerbBuilder | null;

function createPresentContinuousContext(verbDictForm: string): MaybePresentContinuousContext {
    let verbDictFormBase = VERB_PRESENT_CONT_BASE_MAP.get(verbDictForm);
    if (verbDictFormBase != null) {
        return new PresentContinuousContext(verbDictFormBase);
    }
    return null;
}

class VerbBuilder {
    verbDictForm: string
    verbBase: string
    needsYaSuffix: boolean
    baseLast: string
    soft: boolean
    softOffset: number
    contContext: MaybePresentContinuousContext
    wantAuxBuilder: MaybeVerbBuilder
    canAuxBuilder: MaybeVerbBuilder
    defaultContinuousBuilder: MaybeVerbBuilder
    constructor(verbDictForm: string, forceExceptional = false) {
        if (!validateVerb(verbDictForm)) {
            throw new Error("verb dictionary form must end with -у/-ю");
        }
        this.verbDictForm = verbDictForm;
        this.verbBase = chopLast(verbDictForm, 1);
        this.needsYaSuffix = false;
        this.soft = (
            wordIsSoft(this.verbBase)
            || FORCED_SOFT_VERBS.has(verbDictForm)
        );
        this.softOffset = this.soft ? 1 : 0;

        /* exceptions */
        if (isVerbException(verbDictForm) || (isVerbOptionalException(verbDictForm) && forceExceptional)) {
            this.verbBase = this.verbBase + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
        } else if (isVerbException2(verbDictForm)) {
            this.verbBase = this.verbBase + "й" + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
        } else if (verbDictForm.endsWith("ю")) {
            if (verbDictForm.endsWith("ию")) {
                if (!this.soft) {
                    this.needsYaSuffix = true;
                } else {
                    // nothing special here
                }
            } else {
                this.verbBase = this.verbBase + "й";
            }
        }

        this.baseLast = getLastItem(this.verbBase);

        this.contContext = createPresentContinuousContext(verbDictForm);
        this.wantAuxBuilder = null;
        this.canAuxBuilder = null;
        this.defaultContinuousBuilder = null;
    }
    getPersAffix1ExceptThirdPerson(person: GrammarPerson, number: GrammarNumber): string {
        if (person == "Third") {
            return "";
        }
        return VERB_PERS_AFFIXES1[person][number][this.softOffset];
    }
    /* used only for Statement/Question sentence types */
    presentTransitiveSuffix(): string {
        if (this.needsYaSuffix) {
            return "я";
        }
        if (genuineVowel(this.baseLast)) {
            return "й";
        }
        if (this.soft) {
            return "е";
        }
        return "а";
    }
    possibleFutureSuffix(): string {
        if (genuineVowel(this.baseLast)) {
            return "р";
        }
        if (this.soft) {
            return "ер";
        }
        return "ар";
    }
    possibleFutureBaseWithSuffix(): string {
        let affix = this.possibleFutureSuffix();
        if (this.baseLast == "й" && affix == "ар") {
            let chopped = chopLast(this.verbBase, 1)
            return `${chopped}яр`;
        }
        return `${this.verbBase}${affix}`;
    }
    getNegativeBaseOf(base: string): string {
        let baseLast = getLastItem(base);
        let particle = getQuestionParticle(baseLast, this.softOffset);
        return `${base}${particle}`;
    }
    getNegativeBase(): string {
        return this.getNegativeBaseOf(this.verbBase);
    }
    getQuestionForm(phrase: string): string {
        let particle = getQuestionParticle(getLastItem(phrase), this.softOffset);
        return `${phrase} ${particle}?`;
    }
    getPastBase(): string {
        let specialPast = VERB_EXCEPTION_VOWEL_IN_PAST_MAP.get(this.verbDictForm)
        if (specialPast != null) {
            return specialPast
        }
        return `${chopLast(this.verbBase, 1)}${fixGgbInPastBase(this.baseLast)}`;
    }
    /* Ауыспалы осы/келер шақ */
    presentTransitiveForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (sentenceType == "Statement") {
            let affix = this.presentTransitiveSuffix();
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.softOffset];
            return fixShortIBigrams(`${this.verbBase}${affix}${persAffix}`);
        } else if (sentenceType == "Negative") {
            let negativeBase = this.getNegativeBase();
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.softOffset];
            return fixBgBigrams(`${negativeBase}й${persAffix}`);
        } else if (sentenceType == "Question") {
            let affix = this.presentTransitiveSuffix();
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            return fixShortIBigrams(this.getQuestionForm(`${this.verbBase}${affix}${persAffix}`));
        }
        return NOT_SUPPORTED;
    }
    /* Нақ осы шақ */
    presentSimpleContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (this.contContext == null) {
            return NOT_SUPPORTED;
        }
        if (sentenceType == "Statement") {
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            return `${this.contContext.verbBase}${persAffix}`;
        } else if (sentenceType == "Negative") {
            let affix = getGangenKanken(this.baseLast, this.softOffset);

            // parameters of "жоқ", not of the verb base
            let gokLast = 'қ';
            let gokSoftOffset = 0;

            let persAffix = getPersAffix1(person, number, gokLast, gokSoftOffset);
            return `${this.verbBase}${affix} жоқ${persAffix}`;
        } else if (sentenceType == "Question") {
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            return this.getQuestionForm(`${this.contContext.verbBase}${persAffix}`);
        }
        return NOT_SUPPORTED;
    }
    getPresentContinuousBase(): string {
        if (VERB_PRESENT_CONT_EXCEPTION_U_SET.has(this.verbDictForm)) {
            return chopLast(this.verbBase, 1) + "у";
        }
        return this.verbBase;
    }
    getPresentContinousAffix(): string {
        if (VERB_PRESENT_CONT_EXCEPTION_A_SET.has(this.verbDictForm)) {
            return "а";
        }
        if (VERB_PRESENT_CONT_EXCEPTION_E_SET.has(this.verbDictForm)) {
            return "е";
        }
        if (genuineVowel(this.baseLast)) {
            return "п";
        }
        if (this.soft) {
            return "іп";
        }
        return "ып";
    }
    presentContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, auxBuilder: VerbBuilder): string {
        if (auxBuilder.contContext == null) {
            return NOT_SUPPORTED;
        }
        const aeException = VERB_PRESENT_CONT_EXCEPTION_A_SET.has(this.verbDictForm) || VERB_PRESENT_CONT_EXCEPTION_E_SET.has(this.verbDictForm);
        if (aeException && auxBuilder.verbDictForm != VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED) {
            return NOT_SUPPORTED;
        }
        const verbBase = this.getPresentContinuousBase();
        const affix = this.getPresentContinousAffix();
        const auxVerb = auxBuilder.presentSimpleContinuousForm(person, number, sentenceType);
        return `${verbBase}${affix} ${auxVerb}`;
    }
    /* XXX should this form be used by default? */
    presentContinuousSimpleNegativeForm(person: GrammarPerson, number: GrammarNumber, auxBuilder: VerbBuilder): string {
        if (auxBuilder.contContext == null) {
            return NOT_SUPPORTED;
        }
        let negativeBase = this.getNegativeBase();
        const auxVerb = auxBuilder.presentSimpleContinuousForm(person, number, SentenceType.Statement);
        return fixBgBigrams(`${negativeBase}й ${auxVerb}`);
    }
    getDefaultContinuousBuilder() {
        if (this.defaultContinuousBuilder == null) {
            this.defaultContinuousBuilder = new VerbBuilder("жату");
        }
        return this.defaultContinuousBuilder;
    }
    getFormByShak(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): string {
        if (shak == "PresentTransitive") {
            return this.presentTransitiveForm(person, number, sentenceType);
        } else if (shak == "PresentContinuous") {
            if (sentenceType == SentenceType.Negative) {
                return this.presentContinuousSimpleNegativeForm(person, number, this.getDefaultContinuousBuilder());
            }
            return this.presentContinuousForm(person, number, sentenceType, this.getDefaultContinuousBuilder());
        }
        return NOT_SUPPORTED;
    }
    /* Қалау рай */
    getWantAuxBuilder(): VerbBuilder {
        if (this.wantAuxBuilder == null) {
            this.wantAuxBuilder = new VerbBuilder("келу");
        }
        return this.wantAuxBuilder;
    }
    getWantAuxVerb(sentenceType: SentenceType, shak: VerbShak): string {
        if (shak == VerbShak.PresentContinuous && sentenceType != SentenceType.Negative) {
            let contAuxVerb = this.getDefaultContinuousBuilder().presentSimpleContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, sentenceType);
            return `келіп ${contAuxVerb}`;
        }
        return this.getWantAuxBuilder().getFormByShak(GrammarPerson.Third, GrammarNumber.Singular, sentenceType, shak);
    }
    wantClause(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): string {
        let affix = getGygiKyki(this.baseLast, this.softOffset);
        let partial = fixXkBigrams(`${this.verbBase}${affix}`);
        let persAffix = VERB_WANT_PERS_AFFIXES[person][number][this.softOffset];
        let auxVerb = this.getWantAuxVerb(sentenceType, shak);
        return `${partial}${persAffix} ${auxVerb}`;
    }

    getCanAuxBuilder(): VerbBuilder {
        if (this.canAuxBuilder == null) {
            this.canAuxBuilder = new VerbBuilder("алу");
        }
        return this.canAuxBuilder;
    }
    canClause(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): string {
        let affix = this.presentTransitiveSuffix();
        let verb = fixShortIBigrams(`${this.verbBase}${affix}`);
        let auxVerb = this.getCanAuxBuilder().getFormByShak(person, number, sentenceType, shak);
        return `${verb} ${auxVerb}`;
    }
    /* Жедел өткен шақ */
    pastForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        let persAffix = VERB_PERS_AFFIXES2[person][number][this.softOffset];
        if (sentenceType == SentenceType.Statement) {
            let pastBase = this.getPastBase()
            let pastBaseLast = getLastItem(pastBase)
            let affix = getDydiTyti(pastBaseLast, this.softOffset);
            return `${pastBase}${affix}${persAffix}`;
        } else if (sentenceType == SentenceType.Negative) {
            let pastBase = this.getPastBase()
            let negativeBase = this.getNegativeBaseOf(pastBase);
            let affix = DYDI[this.softOffset];
            return fixBgBigrams(`${negativeBase}${affix}${persAffix}`);
        } else if (sentenceType == SentenceType.Question) {
            let affix = getDydiTyti(this.baseLast, this.softOffset);
            return this.getQuestionForm(`${this.verbBase}${affix}${persAffix}`);
        }
        return NOT_SUPPORTED;
    }
    /* Болжалды келер шақ */
    possibleFutureForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (sentenceType == SentenceType.Statement) {
            let baseWithSuffix = this.possibleFutureBaseWithSuffix();
            let affixLast = getLastItem(baseWithSuffix);
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return `${baseWithSuffix}${persAffix}`;
        } else if (sentenceType == SentenceType.Negative) {
            let negativeBase = this.getNegativeBase();
            let formSuffix = "с";
            let persAffix = getPersAffix1(person, number, formSuffix, this.softOffset);
            return fixBgBigrams(`${negativeBase}${formSuffix}${persAffix}`);
        } else if (sentenceType == SentenceType.Question) {
            let baseWithSuffix = this.possibleFutureBaseWithSuffix();
            let affixLast = getLastItem(baseWithSuffix);
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return this.getQuestionForm(`${baseWithSuffix}${persAffix}`);
        }
        return NOT_SUPPORTED;
    }
    /* Мақсатты келер шақ */
    intentionFutureForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): string {
        if (sentenceType == SentenceType.Statement) {
            let tenseAffix = getIntentionFutureAffix(this.baseLast, this.softOffset);
            let affixLast = getLastItem(tenseAffix);
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return `${this.verbBase}${tenseAffix}${persAffix}`;
        } else if (sentenceType == SentenceType.Negative) {
            let tenseAffix = getIntentionFutureAffix(this.baseLast, this.softOffset);
            // last sound and softness come from "емес"
            let persAffix = getPersAffix1(person, number, "с", 1);
            return `${this.verbBase}${tenseAffix} емес${persAffix}`;
        } else if (sentenceType == SentenceType.Question) {
            let tenseAffix = getIntentionFutureAffix(this.baseLast, this.softOffset);
            let affixLast = getLastItem(tenseAffix);
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return this.getQuestionForm(`${this.verbBase}${tenseAffix}${persAffix}`);
        }
        return NOT_SUPPORTED;
    }
}
