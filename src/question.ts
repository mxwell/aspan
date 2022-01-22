function mameCompatible(c: string): boolean {
    return vowel(c) || checkCharPresence(c, CONS_GROUP1);
}

function babeCompatible(c: string): boolean {
    return checkCharPresence(c, CONS_GROUP2);
}

function getQuestionParticle(c: string, soft_offset: number): string {
    if (mameCompatible(c)) {
        return MAME[soft_offset];
    }
    if (babeCompatible(c)) {
        return BABE[soft_offset];
    }
    return PAPE[soft_offset];
}