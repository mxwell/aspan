/* TODO find a reference, current implementation is just a guess */
function gangenCompatible(c: string): boolean {
    return vowel(c) || checkCharPresence(c, CONS_GROUP1) || checkCharPresence(c, CONS_GROUP2);
}

function getGangenKanken(c: string, soft_offset: number): string {
    if (gangenCompatible(c)) {
        return GANGEN[soft_offset];
    }
    return KANKEN[soft_offset];
}
