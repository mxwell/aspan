/* TODO find a reference, current implementation is just a guess */
function gangenCompatible(c: string): boolean {
    return vowel(c) || checkCharPresence(c, CONS_GROUP1) || checkCharPresence(c, CONS_GROUP2);
}

function getGangenKanken(c: string, softOffset: number): string {
    if (gangenCompatible(c)) {
        return GANGEN[softOffset];
    }
    return KANKEN[softOffset];
}

function kykiCompatible(c: string): boolean {
    return checkCharPresence(c, CONS_GROUP4) || checkCharPresence(c, CONS_GROUP5);
}

function getGygiKyki(c: string, softOffset: number): string {
    if (kykiCompatible(c)) {
        return KYKI[softOffset];
    }
    return GYGI[softOffset];
}

function tytiCompatible(c: string): boolean {
    return kykiCompatible(c);
}

function getDydiTyti(c: string, softOffset: number): string {
    if (tytiCompatible(c)) {
        return TYTI[softOffset];
    }
    return DYDI[softOffset];
}