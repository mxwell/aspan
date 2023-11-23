function mameCompatible(c: string): boolean {
    return vowel(c) || checkCharPresence(c, CONS_GROUP1);
}

function babeCompatible(c: string): boolean {
    return checkCharPresence(c, CONS_GROUP2);
}

function chooseMBP(c: string, soft_offset: number, m_affixes: string[], b_affixes: string[], p_affixes: string[]): string {
    if (mameCompatible(c)) {
        return m_affixes[soft_offset];
    }
    if (babeCompatible(c)) {
        return b_affixes[soft_offset];
    }
    return p_affixes[soft_offset];
}