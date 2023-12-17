function getImperativeVowel(person: GrammarPerson, number: GrammarNumber, c: string, softOffset: number): string {
    if (person == GrammarPerson.First) {
        if (genuineVowel(c)) {
            return "";
        }
        return AE[softOffset];
    }
    if (person == GrammarPerson.Second) {
        if (number == GrammarNumber.Singular || genuineVowel(c)) {
            return "";
        }
        return YI[softOffset];
    }
    if (person == GrammarPerson.SecondPolite) {
        if (genuineVowel(c)) {
            return "";
        }
        return YI[softOffset];
    }
    return "";
}

function getImperativeAffix(person: GrammarPerson, number: GrammarNumber, c: string, softOffset: number): string {
    let vowel = getImperativeVowel(person, number, c, softOffset);
    let affix = IMPERATIVE_AFFIXES[person][number][softOffset];
    return `${vowel}${affix}`;
}