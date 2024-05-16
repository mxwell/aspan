enum PHRASAL_PART_TYPE {
    Unclassified = "Unclassified",
    Space = "Space",
    Punctuation = "Punctuation",
    VerbBase = "VerbBase",
    VerbTenseAffix = "VerbTenseAffix",
    VerbPersonalAffix = "VerbPersonalAffix",
    VerbNegation = "VerbNegation",
    QuestionParticle = "QuestionParticle",
    NounBase = "NounBase",
    PluralAffix = "PluralAffix",
    PossessiveAffix = "PossessiveAffix",
}

enum PART_EXPLANATION_TYPE {
    // VerbBase
    VerbBaseStripU = "VerbBaseStripU",
    VerbBaseLostIShort = "VerbBaseLostIShort",
    VerbBaseLostY = "VerbBaseLostY",
    VerbBaseGainedY = "VerbBaseGainedY",
    VerbBaseGainedIShort = "VerbBaseGainedIShort",
    VerbBaseGainIShortLoseY = "VerbBaseGainIShortLoseY",
    VerbBaseGainedIShortY = "VerbBaseGainedIShortY",
    VerbBaseGainedYInsidePriorCons = "VerbBaseGainedYInsidePriorCons",
    VerbBaseReplaceB2U = "VerbBaseReplaceB2U",
    VerbBaseReplaceLastCons = "VerbBaseReplaceLastCons",
    // .. here go other base modifications

    // Negation particles
    VerbNegationPostBase = "VerbNegationPostBase",

    // VerbTenseAffix
    VerbTenseAffixPresentTransitive = "VerbTenseAffixPresentTransitive",
    VerbTenseAffixPresentTransitiveToYa = "VerbTenseAffixPresentTransitiveToYa",
    VerbTenseAffixPresentTransitiveToYi = "VerbTenseAffixPresentTransitiveToYi",

    // VerbPersonalAffix
    VerbPersonalAffixPresentTransitive = "VerbPersonalAffixPresentTransitive",
    VerbPersonalAffixPresentTransitiveQuestionSkip = "VerbPersonalAffixPresentTransitiveQuestionSkip",

    // QuestionParticle
    QuestionParticleSeparate = "QuestionParticleSeparate",
}

class PartExplanation {
    explanationType: PART_EXPLANATION_TYPE;
    soft: boolean;
    softPos: number;
    variant: number;

    constructor(explanationType: PART_EXPLANATION_TYPE, soft: boolean, softPos: number = -1, variant: number = -1) {
        this.explanationType = explanationType;
        this.soft = soft;
        this.softPos = softPos;
        this.variant = variant;
    }
}

type MaybePartExplanation = PartExplanation | null;

class PhrasalPart {
    partType: PHRASAL_PART_TYPE;
    content: string;
    aux: boolean;
    explanation: MaybePartExplanation;

    constructor(partType: PHRASAL_PART_TYPE, content: string, aux: boolean = false, explanation: MaybePartExplanation = null) {
        this.partType = partType;
        this.content = content;
        this.aux = aux;
        this.explanation = explanation;
    }
}

type MaybePhrasal = Phrasal | null;

class Phrasal {
    parts: PhrasalPart[];
    raw: string;
    forbidden: boolean;
    alternative: MaybePhrasal;

    constructor(parts: PhrasalPart[], raw: string, forbidden: boolean, alternative: MaybePhrasal) {
        this.parts = parts;
        this.raw = raw;
        this.forbidden = forbidden;
        this.alternative = alternative;
    }
}

const NOT_SUPPORTED: string = "<not supported>";
const NOT_SUPPORTED_PHRASAL = new Phrasal([], NOT_SUPPORTED, false, null);

class PhrasalBuilder {
    private parts: PhrasalPart[];
    private forbidden: boolean;
    private alternative: MaybePhrasal;

    constructor() {
        this.parts = [];
        this.alternative = null;
    }
    addPart(part: PhrasalPart, allowEmpty: boolean = false): PhrasalBuilder {
        if (part.content.length > 0 || allowEmpty) {
            this.parts.push(part);
        }
        return this;
    }
    unclassified(s: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.Unclassified, s)
        );
    }
    space(): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.Space, " ")
        );
    }
    punctuation(mark: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.Punctuation, mark)
        );
    }
    verbBase(verbBase: string) {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbBase, verbBase)
        );
    }
    verbBaseWithExplanation(verbBase: string, explanation: PartExplanation): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbBase, verbBase, false, explanation)
        );
    }
    tenseAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbTenseAffix, affix)
        );
    }
    tenseAffixWithExplanation(affix: string, explanation: PartExplanation): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbTenseAffix, affix, false, explanation)
        );
    }
    personalAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbPersonalAffix, affix)
        );
    }
    personalAffixWithExplanation(affix: string, explanation: PartExplanation): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbPersonalAffix, affix, false, explanation),
            /* allowEmpty */ true
        );
    }
    negation(particle: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbNegation, particle)
        );
    }
    negationWithExplanation(particle: string, explanation: PartExplanation): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbNegation, particle, false, explanation)
        );
    }
    questionParticle(particle: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.QuestionParticle, particle)
        );
    }
    questionParticleWithExplanation(particle: string, explanation: PartExplanation): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.QuestionParticle, particle, false, explanation)
        );
    }
    auxVerb(phrasal: Phrasal): PhrasalBuilder {
        for (let i = 0; i < phrasal.parts.length; ++i) {
            let part = phrasal.parts[i];
            this.addPart(
                new PhrasalPart(part.partType, part.content, /* aux */ true)
            );
        }
        return this;
    }
    nounBase(nounBase: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.NounBase, nounBase)
        );
    }
    pluralAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.PluralAffix, affix)
        );
    }
    possessiveAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.PossessiveAffix, affix)
        );
    }
    setForbidden(forbidden: boolean): PhrasalBuilder {
        this.forbidden = forbidden;
        return this;
    }
    markForbidden(): PhrasalBuilder {
        return this.setForbidden(true);
    }
    addAlternative(alternative: Phrasal): PhrasalBuilder {
        this.alternative = alternative;
        return this;
    }
    getLastItem(): string {
        let parts = this.parts;
        let index = parts.length - 1;
        while (index > 0 && parts[index].content.length === 0) {
            index--;
        }
        return getLastItem(parts[index].content);
    }
    build(): Phrasal {
        let partStrings: string[] = [];
        for (let i = 0; i < this.parts.length; ++i) {
            partStrings.push(`${this.parts[i].content}`);
        }
        return new Phrasal(
            this.parts,
            partStrings.join(""),
            this.forbidden,
            this.alternative,
        );
    }
}