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

class BaseAndLast {
    base: string;
    last: string;
    constructor(base: string, last: string) {
        this.base = base;
        this.last = last;
    }
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
    buildQuestionForm(builder: PhrasalBuilder): PhrasalBuilder {
        let last = builder.getLastItem();
        let particle = getQuestionParticle(last, this.softOffset);
        return builder
            .space()
            .questionParticle(particle)
            .punctuation("?");
    }
    buildUnclassified(phrase: string): Phrasal {
        return new PhrasalBuilder()
            .unclassified(phrase)
            .build();
    }
    fixUpSpecialBaseForConsonant(): string {
        let specialBase = VERB_EXCEPTION_ADD_VOWEL_MAP.get(this.verbDictForm)
        if (specialBase != null) {
            return specialBase
        }
        return this.verbBase;
    }
    presentTransitiveCommonBuilder(): PhrasalBuilder {
        var verbBase = this.verbBase;
        var affix = this.presentTransitiveSuffix();
        if (verbBase.endsWith("й") && affix == "а") {
            verbBase = chopLast(verbBase, 1);
            affix = "я";
        } else if ((verbBase.endsWith("ы") || verbBase.endsWith("і")) && affix == "й") {
            verbBase = chopLast(verbBase, 1);
            affix = "и";
        }
        return new PhrasalBuilder()
            .verbBase(verbBase)
            .tenseAffix(affix);
    }
    fixUpBaseForConsonant(base: string, last: string): BaseAndLast {
        let replacement = VERB_LAST_NEGATIVE_CONVERSION.get(last);
        if (replacement != null) {
            return new BaseAndLast(
                replaceLast(base, replacement),
                replacement
            );
        }
        return new BaseAndLast(base, last);
    }
    /* Ауыспалы осы/келер шақ */
    presentTransitiveForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == "Statement") {
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.softOffset];
            return this.presentTransitiveCommonBuilder()
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == "Negative") {
            let pastBase = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(pastBase, getLastItem(pastBase));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let persAffix = VERB_PERS_AFFIXES1[person][number][this.softOffset];
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix("й")
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == "Question") {
            let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
            return this.buildQuestionForm(
                    this.presentTransitiveCommonBuilder()
                        .personalAffix(persAffix)
                ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    presentSimpleContinuousCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (this.contContext == null) {
            return new PhrasalBuilder();
        }
        let persAffix = this.getPersAffix1ExceptThirdPerson(person, number);
        return new PhrasalBuilder()
            .verbBase(this.contContext.verbBase)
            .personalAffix(persAffix);
    }
    /* Нақ осы шақ */
    presentSimpleContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (this.contContext == null) {
            return NOT_SUPPORTED_PHRASAL;
        }
        if (sentenceType == "Statement") {
            return this.presentSimpleContinuousCommonBuilder(person, number)
                .build();
        } else if (sentenceType == "Negative") {
            let affix = getGangenKanken(this.baseLast, this.softOffset);

            // parameters of "жоқ", not of the verb base
            let gokLast = 'қ';
            let gokSoftOffset = 0;

            let persAffix = getPersAffix1(person, number, gokLast, gokSoftOffset);
            return new PhrasalBuilder()
                .verbBase(this.verbBase)
                .tenseAffix(affix)
                .space()
                .negation("жоқ")
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == "Question") {
            return this.buildQuestionForm(
                    this.presentSimpleContinuousCommonBuilder(person, number)
                ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    getPresentContinuousBase(): string {
        if (VERB_PRESENT_CONT_EXCEPTION_U_SET.has(this.verbDictForm)) {
            return replaceLast(this.verbBase, "у");
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
    presentContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, auxBuilder: VerbBuilder): Phrasal {
        if (auxBuilder.contContext == null) {
            return NOT_SUPPORTED_PHRASAL;
        }
        const aeException = VERB_PRESENT_CONT_EXCEPTION_A_SET.has(this.verbDictForm) || VERB_PRESENT_CONT_EXCEPTION_E_SET.has(this.verbDictForm);
        if (aeException && auxBuilder.verbDictForm != VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED) {
            return NOT_SUPPORTED_PHRASAL;
        }
        const verbBase = this.getPresentContinuousBase();
        const affix = this.getPresentContinousAffix();
        const auxVerbPhrasal = auxBuilder.presentSimpleContinuousForm(person, number, sentenceType);
        return new PhrasalBuilder()
            .verbBase(verbBase)
            .tenseAffix(affix)
            .space()
            .auxVerb(auxVerbPhrasal)
            .build();
    }
    /* XXX should this form be used by default? */
    presentContinuousSimpleNegativeForm(person: GrammarPerson, number: GrammarNumber, auxBuilder: VerbBuilder): Phrasal {
        if (auxBuilder.contContext == null) {
            return NOT_SUPPORTED_PHRASAL;
        }
        let negativeBase = this.getNegativeBase();
        const auxVerb = auxBuilder.presentSimpleContinuousForm(person, number, SentenceType.Statement).raw;
        let res = fixBgBigrams(`${negativeBase}й ${auxVerb}`);
        return this.buildUnclassified(res);
    }
    getDefaultContinuousBuilder() {
        if (this.defaultContinuousBuilder == null) {
            this.defaultContinuousBuilder = new VerbBuilder("жату");
        }
        return this.defaultContinuousBuilder;
    }
    getFormByShak(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): Phrasal {
        if (shak == "PresentTransitive") {
            return this.presentTransitiveForm(person, number, sentenceType);
        } else if (shak == "PresentContinuous") {
            if (sentenceType == SentenceType.Negative) {
                return this.presentContinuousSimpleNegativeForm(person, number, this.getDefaultContinuousBuilder());
            }
            return this.presentContinuousForm(person, number, sentenceType, this.getDefaultContinuousBuilder());
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    /* Қалау рай */
    getWantAuxBuilder(): VerbBuilder {
        if (this.wantAuxBuilder == null) {
            this.wantAuxBuilder = new VerbBuilder("келу");
        }
        return this.wantAuxBuilder;
    }
    getWantAuxVerb(sentenceType: SentenceType, shak: VerbShak): Phrasal {
        if (shak == VerbShak.PresentContinuous && sentenceType != SentenceType.Negative) {
            let contAuxVerb = this.getDefaultContinuousBuilder().presentSimpleContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, sentenceType).raw;
            let res = `келіп ${contAuxVerb}`;
            return this.buildUnclassified(res);
        }
        return this.getWantAuxBuilder().getFormByShak(GrammarPerson.Third, GrammarNumber.Singular, sentenceType, shak);
    }
    wantClause(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): Phrasal {
        let specialBase = this.fixUpSpecialBaseForConsonant();
        let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
        let affix = getGygiKyki(baseAndLast.last, this.softOffset);
        let persAffix = VERB_WANT_PERS_AFFIXES[person][number][this.softOffset];
        let auxVerbPhrasal = this.getWantAuxVerb(sentenceType, shak);
        return new PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix)
            .personalAffix(persAffix)
            .space()
            .auxVerb(auxVerbPhrasal)
            .build();
    }

    getCanAuxBuilder(): VerbBuilder {
        if (this.canAuxBuilder == null) {
            this.canAuxBuilder = new VerbBuilder("алу");
        }
        return this.canAuxBuilder;
    }
    canClause(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, shak: VerbShak): Phrasal {
        let auxVerbPhrasal = this.getCanAuxBuilder().getFormByShak(person, number, sentenceType, shak);
        return this.presentTransitiveCommonBuilder()
            .space()
            .auxVerb(auxVerbPhrasal)
            .build();
    }
    pastCommonBuilder(): PhrasalBuilder {
        let pastBase = this.fixUpSpecialBaseForConsonant();
        let baseAndLast = this.fixUpBaseForConsonant(pastBase, getLastItem(pastBase));
        let affix = getDydiTyti(baseAndLast.last, this.softOffset);
        return new PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix);
    }
    /* Жедел өткен шақ */
    pastForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        let persAffix = VERB_PERS_AFFIXES2[person][number][this.softOffset];
        if (sentenceType == SentenceType.Statement) {
            return this.pastCommonBuilder()
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let pastBase = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(pastBase, getLastItem(pastBase));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let affix = DYDI[this.softOffset];
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.pastCommonBuilder()
                        .personalAffix(persAffix)
                ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    possibleFutureCommonBuilder(): PhrasalBuilder {
        var base = this.verbBase;
        var affix = this.possibleFutureSuffix();
        if (base.endsWith("й") && affix == "ар") {
            base = chopLast(base, 1);
            affix = "яр";
        }
        return new PhrasalBuilder()
            .verbBase(base)
            .tenseAffix(affix);
    }
    /* Болжалды келер шақ */
    possibleFutureForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            let builder = this.possibleFutureCommonBuilder();
            let affixLast = builder.getLastItem();
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return builder
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let specialBase = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let affix = "с";
            let persAffix = getPersAffix1(person, number, affix, this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            let builder = this.possibleFutureCommonBuilder();
            let affixLast = builder.getLastItem();
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return this.buildQuestionForm(
                    builder.
                        personalAffix(persAffix)
                ).build()
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    intentionFutureCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        let specialBase = this.fixUpSpecialBaseForConsonant();
        let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
        let tenseAffix = getIntentionFutureAffix(baseAndLast.last, this.softOffset);
        let affixLast = getLastItem(tenseAffix);
        let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
        return new PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(tenseAffix)
            .personalAffix(persAffix);
    }
    /* Мақсатты келер шақ */
    intentionFutureForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            return this.intentionFutureCommonBuilder(person, number)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let specialBase = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
            let tenseAffix = getIntentionFutureAffix(baseAndLast.last, this.softOffset);
            // last sound and softness come from "емес"
            let persAffix = getPersAffix1(person, number, "с", 1);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .tenseAffix(tenseAffix)
                .space()
                .negation("емес")
                .personalAffix(persAffix)
                .build()
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.intentionFutureCommonBuilder(person, number)
                ).build()
        }
        return NOT_SUPPORTED_PHRASAL;
    }
}
