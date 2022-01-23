function getFirstPersAffix1LetterGroup(c: string): PersAffix1LetterGroup {
    if (checkCharPresence(c, CONS_GROUP3)) {
        return PersAffix1LetterGroup.PersAffix1GzGroup;
    }
    if (checkCharPresence(c, CONS_GROUP4) || checkCharPresence(c, CONS_GROUP5)) {
        return PersAffix1LetterGroup.PersAffixUnvoicedGroup;
    }
    return PersAffix1LetterGroup.PersAffix1DefaultGroup;
}

function getFirstPersAffix1(number: GrammarNumber, c: string, softOffset: number): string {
    const group = getFirstPersAffix1LetterGroup(c);
    return FIRST_PERS_AFFIXES1[number][group][softOffset];
}

function getPersAffix1(person: GrammarPerson, number: GrammarNumber, c: string, softOffset: number): string {
    if (person == GrammarPerson.First) {
        return getFirstPersAffix1(number, c, softOffset);
    }
    if (person == GrammarPerson.Second) {
        return SECOND_PERS_AFFIXES1[number][softOffset];
    }
    if (person == GrammarPerson.SecondPolite) {
        return SECOND_POLITE_PERS_AFFIXES1[number][softOffset];
    }
    return "";
}
