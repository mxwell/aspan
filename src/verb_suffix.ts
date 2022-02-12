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

function kykiCompatible(c: string): boolean {
    return checkCharPresence(c, CONS_GROUP4) || checkCharPresence(c, CONS_GROUP5);
}

function getGygiKyki(c: string, soft_offset: number): string {
    if (kykiCompatible(c)) {
        return KYKI[soft_offset];
    }
    return GYGI[soft_offset];
}

function tytiCompatible(c: string): boolean {
    return kykiCompatible(c);
}

function getDydiTyti(c: string, soft_offset: number): string {
    if (tytiCompatible(c)) {
        return TYTI[soft_offset];
    }
    return DYDI[soft_offset];
}