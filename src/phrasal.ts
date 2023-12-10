enum PHRASAL_PART_TYPE {
    Unclassified = "Unclassified",
    Space = "Space",
    Punctuation = "Punctuation",
    VerbBase = "VerbBase",
    VerbTenseAffix = "VerbTenseAffix",
    VerbPersonalAffix = "VerbPersonalAffix",
    VerbNegation = "VerbNegation",
    QuestionParticle = "QuestionParticle",
    AuxVerb = "AuxVerb",
}

class PhrasalPart {
    partType: PHRASAL_PART_TYPE;
    content: string;

    constructor(partType: PHRASAL_PART_TYPE, content: string) {
        this.partType = partType;
        this.content = content;
    }
}

class Phrasal {
    parts: PhrasalPart[];
    raw: string;

    constructor(parts: PhrasalPart[], raw: string) {
        this.parts = parts;
        this.raw = raw;
    }
}

const NOT_SUPPORTED: string = "<not supported>";
const NOT_SUPPORTED_PHRASAL = new Phrasal([], NOT_SUPPORTED);

class PhrasalBuilder {
    private parts: PhrasalPart[];

    constructor() {
        this.parts = [];
    }
    addPart(part: PhrasalPart): PhrasalBuilder {
        if (part.content.length > 0) {
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
    tenseAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbTenseAffix, affix)
        );
    }
    personalAffix(affix: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbPersonalAffix, affix)
        );
    }
    negation(particle: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.VerbNegation, particle)
        );
    }
    questionParticle(particle: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.QuestionParticle, particle)
        );
    }
    auxVerb(verb: string): PhrasalBuilder {
        return this.addPart(
            new PhrasalPart(PHRASAL_PART_TYPE.AuxVerb, verb)
        );
    }
    getLastItem(): string {
        let parts = this.parts;
        return getLastItem(parts[parts.length - 1].content);
    }
    build(): Phrasal {
        let partStrings: string[] = [];
        for (let i = 0; i < this.parts.length; ++i) {
            partStrings.push(`${this.parts[i].content}`);
        }
        return new Phrasal(
            this.parts,
            partStrings.join("")
        );
    }
}