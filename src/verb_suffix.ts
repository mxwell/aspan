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

function getGalygeliKalykeli(c: string, softOffset: number): string {
    if (gangenCompatible(c)) {
        return GALYGELI[softOffset];
    }
    return KALYKELI[softOffset];
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

function getDydiTyti(c: string, softOffset: number): string {
    if (kykiCompatible(c)) {
        return TYTI[softOffset];
    }
    return DYDI[softOffset];
}

function getYpip(c: string, softOffset: number): string {
    if (genuineVowel(c)) {
        return "п";
    }
    return YPIP[softOffset];
}

function getSase(softOffset: number): string {
    return SASE[softOffset];
}