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
    const lastWord = getLastWord(verbDictForm);
    return VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET.has(lastWord);
}

type MaybeMeanings = string[][] | null;

function getOptExceptVerbMeanings(verbDictForm: string): MaybeMeanings {
    const lastWord = getLastWord(verbDictForm);
    return OPT_EXCEPT_VERB_MEANINGS.get(lastWord);
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

const HARD_OFFSET = 0;
const SOFT_OFFSET = 1;

function createPresentContinuousContext(verbDictForm: string): MaybePresentContinuousContext {
    let verbDictFormBase = VERB_PRESENT_CONT_BASE_MAP.get(verbDictForm);
    if (verbDictFormBase != null) {
        return new PresentContinuousContext(verbDictFormBase);
    }
    return null;
}

class BaseAndExplanationType {
    base: string;
    last: string;
    explanationType: PART_EXPLANATION_TYPE;
    constructor(base: string, last: string, explanationType: PART_EXPLANATION_TYPE) {
        this.base = base;
        this.last = last;
        this.explanationType = explanationType;
    }
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
    baseExplanation: PART_EXPLANATION_TYPE
    forceExceptional: boolean
    regularVerbBase: string
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
        this.baseExplanation = PART_EXPLANATION_TYPE.VerbBaseStripU;
        this.forceExceptional = forceExceptional;
        this.regularVerbBase = this.verbBase;
        this.needsYaSuffix = false;
        this.soft = wordIsSoft(this.verbDictForm);
        this.softOffset = this.soft ? SOFT_OFFSET : HARD_OFFSET;

        /* exceptions */
        if (isVerbException(verbDictForm) || (getOptExceptVerbMeanings(verbDictForm) != null && forceExceptional)) {
            this.verbBase = this.verbBase + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
            this.baseExplanation = PART_EXPLANATION_TYPE.VerbBaseGainedY;
        } else if (isVerbException2(verbDictForm)) {
            this.verbBase = this.verbBase + "й" + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
            this.baseExplanation = PART_EXPLANATION_TYPE.VerbBaseGainedIShortY;
        } else if (verbDictForm.endsWith("ю")) {
            if (verbDictForm.endsWith("ию")) {
                if (!this.soft) {
                    this.needsYaSuffix = true;
                } else {
                    // nothing special here
                }
            } else {
                this.verbBase = this.verbBase + "й";
                this.baseExplanation = PART_EXPLANATION_TYPE.VerbBaseGainedIShort;
            }
        }

        this.baseLast = getLastItem(this.verbBase);

        this.contContext = createPresentContinuousContext(verbDictForm);
        this.wantAuxBuilder = null;
        this.canAuxBuilder = null;
        this.defaultContinuousBuilder = null;
    }
    getPersAffix1ExceptThirdPerson(person: GrammarPerson, number: GrammarNumber, softOffset: number): string {
        if (person == "Third") {
            return "";
        }
        return VERB_PERS_AFFIXES1[person][number][softOffset];
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
    presentColloqShortSuffix(grammarPerson): string {
        if (grammarPerson == GrammarPerson.First || grammarPerson == GrammarPerson.Third) {
            return AFFIX_ATYR[this.softOffset];
        }
        return AFFIX_AT[this.softOffset]
    }
    presentColloqLongSuffix(grammarPerson): string {
        if (grammarPerson == GrammarPerson.First || grammarPerson == GrammarPerson.Third) {
            if (genuineVowel(this.baseLast)) {
                return "ватыр";
            }
            const vowel = YI[this.softOffset];
            return `${vowel}ватыр`;
        }
        if (genuineVowel(this.baseLast)) {
            return "ват";
        }
        const vowel = YI[this.softOffset];
        return `${vowel}ват`;
    }
    presentColloqSuffix(grammarPerson): string {
        if (VERB_PRESENT_CONT_EXCEPTION_E_SET.has(this.verbDictForm) || VERB_PRESENT_CONT_EXCEPTION_A_SET.has(this.verbDictForm)) {
            return this.presentColloqShortSuffix(grammarPerson);
        } else {
            return this.presentColloqLongSuffix(grammarPerson);
        }
    }
    presentColloqNegSuffix(grammarPerson): string {
        if (grammarPerson == GrammarPerson.First || grammarPerson == GrammarPerson.Third) {
            return "ятыр";
        }
        return "ят";
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
    pastTransitiveSuffix(baseLast: string): string {
        if (this.needsYaSuffix) {
            return "ятын";
        }
        if (genuineVowel(baseLast)) {
            if (this.soft) {
                return "йтін"
            }
            return "йтын";
        }
        if (this.soft) {
            return "етін";
        }
        return "атын";
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
    buildQuestionFormGeneric(builder: PhrasalBuilder, soft: boolean, expl: boolean): PhrasalBuilder {
        let last = builder.getLastItem();
        let particle = getQuestionParticle(last, soft ? SOFT_OFFSET : HARD_OFFSET);
        builder.space();
        if (expl) {
            builder.questionParticleWithExplanation(
                particle,
                new PartExplanation(
                    PART_EXPLANATION_TYPE.QuestionParticleSeparate,
                    soft,
                )
            );
        } else {
            builder.questionParticle(particle);
        }
        return builder.punctuation("?");
    }
    buildQuestionForm(builder: PhrasalBuilder): PhrasalBuilder {
        return this.buildQuestionFormGeneric(builder, this.soft, false);
    }
    buildQuestionFormExpl(builder: PhrasalBuilder): PhrasalBuilder {
        return this.buildQuestionFormGeneric(builder, this.soft, true);
    }
    buildUnclassified(phrase: string): Phrasal {
        return new PhrasalBuilder()
            .unclassified(phrase)
            .build();
    }
    genericBaseModifier(nc: boolean, yp: boolean): BaseAndExplanationType {
        if (nc) { /* next is consonant */
            if (yp) {
                throw new Error(`invalid arguments: ${nc}, ${yp}`)
            }
            /* қорқу -> қорық.. */
            let addVowel = VERB_EXCEPTION_ADD_VOWEL_MAP.get(this.verbDictForm);
            if (addVowel) {
                let last = getLastItem(addVowel);
                return new BaseAndExplanationType(addVowel, last, PART_EXPLANATION_TYPE.VerbBaseGainedYInsidePriorCons);
            }
            let last = getLastItem(this.verbBase);
            let replacement = VERB_LAST_NEGATIVE_CONVERSION.get(last);
            if (replacement != null) {
                return new BaseAndExplanationType(
                    replaceLast(this.verbBase, replacement),
                    replacement,
                    PART_EXPLANATION_TYPE.VerbBaseReplaceLastCons
                );
            }
        } else if (yp) { /* next is -ып */
            /* жабу -> жау, except қабу can become қау or қабы (if forceExceptional) */
            if (VERB_PRESENT_CONT_EXCEPTION_U_SET.has(this.verbDictForm) && !this.forceExceptional) {
                let replacement = "у";
                return new BaseAndExplanationType(
                    replaceLast(this.regularVerbBase, replacement),
                    replacement,
                    PART_EXPLANATION_TYPE.VerbBaseReplaceB2U
                );
            }
        }
        return new BaseAndExplanationType(this.verbBase, this.baseLast, this.baseExplanation);
    }
    // TODO replace with genericBaseModifier()
    fixUpSpecialBaseForConsonant(): string {
        let specialBase = VERB_EXCEPTION_ADD_VOWEL_MAP.get(this.verbDictForm)
        if (specialBase != null) {
            return specialBase
        }
        return this.verbBase;
    }
    // TODO do not force exceptional, use fixUpSpecialBaseForConsonant()
    fixUpSpecialBaseForConsonantAndForceExceptional(): string {
        let specialBase = VERB_EXCEPTION_ADD_VOWEL_MAP.get(this.verbDictForm)
        if (specialBase != null) {
            return specialBase
        }
        let meanings = getOptExceptVerbMeanings(this.verbDictForm);
        if (meanings != null) {
            return this.verbBase + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
        }
        return this.verbBase;
    }
    // TODO replace with genericBaseModifier()
    fixUpSpecialBaseForceExceptional(): string {
        let meanings = getOptExceptVerbMeanings(this.verbDictForm);
        if (meanings != null) {
            return this.verbBase + VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX[this.softOffset];
        }
        return this.verbBase;
    }
    mergeBaseWithVowelAffix(origBase: string, origAffix: string, expl: boolean): PhrasalBuilder {
        var base = origBase;
        var affix = origAffix;
        let baseExplanation = new PartExplanation(
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            this.soft,
            // TODO specify softPos
        );
        let affixExplanation = new PartExplanation(
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            this.soft,
        );
        if (base.endsWith("й") && affix.startsWith("а")) {
            base = chopLast(base, 1);
            baseExplanation.explanationType = PART_EXPLANATION_TYPE.VerbBaseLostIShort;
            affix = replaceFirst(affix, "я");
            affixExplanation.explanationType = PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitiveToYa;
        } else if ((base.endsWith("ы") || base.endsWith("і")) && affix.startsWith("й")) {
            base = chopLast(base, 1);
            baseExplanation.explanationType = (
                this.baseExplanation == PART_EXPLANATION_TYPE.VerbBaseGainedIShortY
                ? PART_EXPLANATION_TYPE.VerbBaseGainIShortLoseY
                : PART_EXPLANATION_TYPE.VerbBaseLostY
            );
            affix = replaceFirst(affix, "и");
            affixExplanation.explanationType = PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitiveToYi;
        }
        if (expl) {
            return new PhrasalBuilder()
                .verbBaseWithExplanation(base, baseExplanation)
                .tenseAffixWithExplanation(affix, affixExplanation);
        } else {
            return new PhrasalBuilder()
                .verbBase(base)
                .tenseAffix(affix);
        }
    }
    presentTransitiveCommonBuilder(): PhrasalBuilder {
        return this.mergeBaseWithVowelAffix(
            this.verbBase,
            this.presentTransitiveSuffix(),
            true
        );
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
    appendPresentTransitivePersAffix(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, builder: PhrasalBuilder): PhrasalBuilder {
        let persAffix = null;
        let explType = null;
        if (sentenceType != SentenceType.Question || person != GrammarPerson.Third) {
            persAffix = VERB_PERS_AFFIXES1[person][number][this.softOffset];
            explType = PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive;
        } else {
            persAffix = "";
            explType = PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitiveQuestionSkip;
        }
        let persAffixExplanation = new PartExplanation(explType, this.soft);
        return builder.personalAffixWithExplanation(persAffix, persAffixExplanation);
    }
    /* Ауыспалы осы/келер шақ */
    presentTransitiveForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == "Statement") {
            return this.appendPresentTransitivePersAffix(
                person, number, sentenceType,
                this.presentTransitiveCommonBuilder()
            ).build();
        } else if (sentenceType == "Negative") {
            let pastBase = this.genericBaseModifier(/* nc */ true, /* yp */ false);
            let baseExplanation = new PartExplanation(
                pastBase.explanationType,
                this.soft,
            );
            let particle = getQuestionParticle(pastBase.last, this.softOffset);
            let particleE = new PartExplanation(
                PART_EXPLANATION_TYPE.VerbNegationPostBase,
                this.soft,
            );
            let affixE = new PartExplanation(
                PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
                this.soft,
            );
            return this.appendPresentTransitivePersAffix(
                person, number, sentenceType,
                new PhrasalBuilder()
                    .verbBaseWithExplanation(pastBase.base, baseExplanation)
                    .negationWithExplanation(particle, particleE)
                    .tenseAffixWithExplanation("й", affixE)
            ).build();
        } else if (sentenceType == "Question") {
            return this.buildQuestionFormExpl(
                this.appendPresentTransitivePersAffix(
                    person, number, sentenceType,
                    this.presentTransitiveCommonBuilder()
                )
            ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    presentSimpleContinuousCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        if (this.contContext == null) {
            return new PhrasalBuilder();
        }
        let persAffix = this.getPersAffix1ExceptThirdPerson(person, number, this.softOffset);
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
    // TODO replace with genericBaseModifier()
    getPresentContinuousBase(): string {
        if (VERB_PRESENT_CONT_EXCEPTION_U_SET.has(this.verbDictForm) && !this.forceExceptional) {
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
        return getYpip(this.baseLast, this.softOffset);
    }
    presentContinuousForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, auxBuilder: VerbBuilder, negateAux: boolean = true): Phrasal {
        if (auxBuilder.contContext == null) {
            return NOT_SUPPORTED_PHRASAL;
        }
        const aeException = VERB_PRESENT_CONT_EXCEPTION_A_SET.has(this.verbDictForm) || VERB_PRESENT_CONT_EXCEPTION_E_SET.has(this.verbDictForm);
        const forbidden = aeException && auxBuilder.verbDictForm != VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED;
        if (sentenceType != SentenceType.Negative || negateAux) {
            const verbBase = this.getPresentContinuousBase();
            const affix = this.getPresentContinousAffix();
            const auxVerbPhrasal = auxBuilder.presentSimpleContinuousForm(person, number, sentenceType);
            return new PhrasalBuilder()
                .verbBase(verbBase)
                .tenseAffix(affix)
                .space()
                .auxVerb(auxVerbPhrasal)
                .setForbidden(forbidden)
                .build();
        } else {
            const verbBase = this.genericBaseModifier(/* nc */ true, /* yp */ false);
            const particle = getQuestionParticle(verbBase.last, this.softOffset);
            const affix = "й";
            const auxVerbPhrasal = auxBuilder.presentSimpleContinuousForm(person, number, SentenceType.Statement);
            return new PhrasalBuilder()
                .verbBase(verbBase.base)
                .negation(particle)
                .tenseAffix(affix)
                .space()
                .auxVerb(auxVerbPhrasal)
                .setForbidden(forbidden)
                .build();
        }
    }
    presentColloquialBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        const verbBase = this.getPresentContinuousBase();
        const affix = this.presentColloqSuffix(person);
        const persAffix = this.getPersAffix1ExceptThirdPerson(person, number, HARD_OFFSET);
        return new PhrasalBuilder()
            .verbBase(verbBase)
            .tenseAffix(affix)
            .personalAffix(persAffix);
    }
    presentColloquialForm(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            return this.presentColloquialBuilder(person, number)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            const verbBase = this.genericBaseModifier(/* nc */ true, /* yp */ false);
            const particle = getColloquialQuestionParticle(verbBase.last, this.softOffset);
            const affix = this.presentColloqNegSuffix(person);
            const persAffix = this.getPersAffix1ExceptThirdPerson(person, number, HARD_OFFSET);
            return new PhrasalBuilder()
                    .verbBase(verbBase.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionFormGeneric(
                    this.presentColloquialBuilder(person, number),
                    false,
                    false
            ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
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
            return this.presentContinuousForm(person, number, sentenceType, this.getDefaultContinuousBuilder(), /* negateAux */ false);
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
    remotePastCommonBuilder(): PhrasalBuilder {
        let specialBase = this.fixUpSpecialBaseForConsonant();
        let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
        let affix = getGangenKanken(baseAndLast.last, this.softOffset);
        return new PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix);
    }
    /* Бұрынғы өткен шақ */
    remotePastTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType, negateAux: boolean = true): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            let builder = this.remotePastCommonBuilder();
            let affixLast = builder.getLastItem();
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return builder
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            if (negateAux) {
                let builder = this.remotePastCommonBuilder();

                // parameters of "жоқ", not of the verb base
                let gokLast = 'қ';
                let gokSoftOffset = 0;

                let persAffix = getPersAffix1(person, number, gokLast, gokSoftOffset);
                return builder
                    .space()
                    .negation("жоқ")
                    .personalAffix(persAffix)
                    .build();
            } else {
                const verbBase = this.genericBaseModifier(/* nc */ true, /* yp */ false);
                const particle = getQuestionParticle(verbBase.last, this.softOffset);
                const particleLast = getLastItem(particle);
                const affix = getGangenKanken(particleLast, this.softOffset);
                const affixLast = getLastItem(affix);
                const persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
                return new PhrasalBuilder()
                    .verbBase(verbBase.base)
                    .negation(particle)
                    .tenseAffix(affix)
                    .personalAffix(persAffix)
                    .build();
            }
        } else if (sentenceType == SentenceType.Question) {
            let builder = this.remotePastCommonBuilder();
            let affixLast = builder.getLastItem();
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return this.buildQuestionForm(
                builder
                    .personalAffix(persAffix)
            ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    pastUncertainCommonBuilder(): PhrasalBuilder {
        let base = this.genericBaseModifier(/* nc */ false, /* yp */ true).base;
        let baseLast = getLastItem(base);
        let affix = getYpip(baseLast, this.softOffset);
        return new PhrasalBuilder()
            .verbBase(base)
            .tenseAffix(affix);
    }
    /* Күмәнді өткен шақ */
    pastUncertainTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        let persAffix = getPersAffix3(person, number, this.softOffset);

        if (sentenceType == SentenceType.Statement) {
            return this.pastUncertainCommonBuilder()
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let baseAndLast = this.genericBaseModifier(/* nc */ true, /* yp */ false);
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let particleLast = getLastItem(particle);
            let affix = getYpip(particleLast, this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                this.pastUncertainCommonBuilder()
                    .personalAffix(persAffix)
            ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    presentParticipleCommonBuilder(expl: boolean): PhrasalBuilder {
        return this.mergeBaseWithVowelAffix(
            this.verbBase, this.pastTransitiveSuffix(this.baseLast), expl
        );
    }
    pastTransitiveCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        let builder = this.presentParticipleCommonBuilder(true);
        let affixLast = builder.getLastItem();
        let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
        return builder
            .personalAffix(persAffix);
    }
    /* Ауыспалы өткен шақ: -атын */
    pastTransitiveTense(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            return this.pastTransitiveCommonBuilder(person, number)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let base = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(base, getLastItem(base));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let particleLast = getLastItem(particle);
            let affix = this.pastTransitiveSuffix(particleLast);
            let affixLast = getLastItem(affix);
            let persAffix = getPersAffix1(person, number, affixLast, this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                this.pastTransitiveCommonBuilder(person, number)
            ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    conditionalMoodCommonBuilder(): PhrasalBuilder {
        let pastBase = this.fixUpSpecialBaseForConsonant();
        let baseAndLast = this.fixUpBaseForConsonant(pastBase, getLastItem(pastBase));
        let affix = getSase(this.softOffset);
        return new PhrasalBuilder()
            .verbBase(baseAndLast.base)
            .tenseAffix(affix);
    }
    /* Шартты рай: -са */
    conditionalMood(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        let persAffix = VERB_PERS_AFFIXES2[person][number][this.softOffset];
        if (sentenceType == SentenceType.Statement) {
            return this.conditionalMoodCommonBuilder()
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let base = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(base, getLastItem(base));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let affix = getSase(this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .personalAffix(persAffix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.conditionalMoodCommonBuilder()
                        .personalAffix(persAffix)
                ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    imperativeMoodCommonBuilder(person: GrammarPerson, number: GrammarNumber): PhrasalBuilder {
        let nc = (
            (person == GrammarPerson.Second && number == GrammarNumber.Singular)
            || person == GrammarPerson.Third
        );
        let baseAndLast = this.genericBaseModifier(nc, /* yp */ false);
        let affix = getImperativeAffix(person, number, baseAndLast.last, this.softOffset);
        return this.mergeBaseWithVowelAffix(baseAndLast.base, affix, false);
    }
    /* Бұйрық рай */
    imperativeMood(person: GrammarPerson, number: GrammarNumber, sentenceType: SentenceType): Phrasal {
        if (sentenceType == SentenceType.Statement) {
            return this.imperativeMoodCommonBuilder(person, number)
                .build();
        } else if (sentenceType == SentenceType.Negative) {
            let base = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(base, getLastItem(base));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let particleLast = getLastItem(particle);
            let affix = getImperativeAffix(person, number, particleLast, this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix)
                .build();
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.imperativeMoodCommonBuilder(person, number)
                ).build();
        }
        return NOT_SUPPORTED_PHRASAL;
    }
    /* Өткен шақ есімше */
    pastParticipleBuilder(sentenceType: SentenceType): MaybePhrasalBuilder {
        if (sentenceType == SentenceType.Statement) {
            return this.remotePastCommonBuilder();
        } else if (sentenceType == SentenceType.Negative) {
            let base = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(base, getLastItem(base));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let particleLast = getLastItem(particle);
            let affix = getGangenKanken(particleLast, this.softOffset);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix);
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.remotePastCommonBuilder()
            );
        }
        return null;
    }
    pastParticiple(sentenceType: SentenceType): Phrasal {
        return finalizeMaybePhrasalBuilder(this.pastParticipleBuilder(sentenceType));
    }
    presentParticipleBuilder(sentenceType: SentenceType): MaybePhrasalBuilder {
        if (sentenceType == SentenceType.Statement) {
            return this.presentParticipleCommonBuilder(false);
        } else if (sentenceType == SentenceType.Negative) {
            let base = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(base, getLastItem(base));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let particleLast = getLastItem(particle);
            let affix = this.pastTransitiveSuffix(particleLast);
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix);
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.presentParticipleCommonBuilder(false)
                );
        }
        return null;
    }
    presentParticiple(sentenceType: SentenceType): Phrasal {
        return finalizeMaybePhrasalBuilder(this.presentParticipleBuilder(sentenceType));
    }
    futureParticipleBuilder(sentenceType: SentenceType): MaybePhrasalBuilder {
        if (sentenceType == SentenceType.Statement) {
            return this.possibleFutureCommonBuilder();
        } else if (sentenceType == SentenceType.Negative) {
            let specialBase = this.fixUpSpecialBaseForConsonant();
            let baseAndLast = this.fixUpBaseForConsonant(specialBase, getLastItem(specialBase));
            let particle = getQuestionParticle(baseAndLast.last, this.softOffset);
            let affix = "с";
            return new PhrasalBuilder()
                .verbBase(baseAndLast.base)
                .negation(particle)
                .tenseAffix(affix);
        } else if (sentenceType == SentenceType.Question) {
            return this.buildQuestionForm(
                    this.possibleFutureCommonBuilder()
                );
        }
        return null;
    }
    futureParticiple(sentenceType: SentenceType): Phrasal {
        return finalizeMaybePhrasalBuilder(this.futureParticipleBuilder(sentenceType));
    }
}
