function mameCompatible(c: string): boolean {
    return vowel(c) || checkCharPresence(c, CONS_GROUP1);
}

function babeCompatible(c: string): boolean {
    return checkCharPresence(c, CONS_GROUP2);
}

function chooseMBP(c: string, softOffset: number, mAffixes: string[], bAffixes: string[], pAffixes: string[]): string {
    if (mameCompatible(c)) {
        return mAffixes[softOffset];
    }
    if (babeCompatible(c)) {
        return bAffixes[softOffset];
    }
    return pAffixes[softOffset];
}

function chooseLDT(c: string, softOffset: number, lAffixes: string[], dAffixes: string[], tAffixes: string[]): string {
    if (vowel(c) || checkCharPresence(c, CONS_GROUP7)) {
        return lAffixes[softOffset];
    }
    if (checkCharPresence(c, CONS_GROUP8)) {
        return dAffixes[softOffset];
    }
    return tAffixes[softOffset];
}
