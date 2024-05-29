function vowel(c: string): boolean {
    return checkCharPresence(c, VOWELS);
}

function genuineVowel(c: string): boolean {
    return checkCharPresence(c, VOWELS_EXCEPT_U_I);
}

enum SoftHardType {
    SOFT_STRONG,
    SOFT_WEAK,
    NEUTRAL,
    HARD_WEAK,
    HARD_STRONG,
};

const VOWELS_BY_SOFT_HARD = new Map<string, SoftHardType>([
    ["ә", SoftHardType.SOFT_STRONG],
    ["е", SoftHardType.SOFT_STRONG],
    ["ө", SoftHardType.SOFT_STRONG],
    ["ү", SoftHardType.SOFT_STRONG],
    ["і", SoftHardType.SOFT_STRONG],

    ["и", SoftHardType.SOFT_WEAK],

    ["ю", SoftHardType.NEUTRAL],
    ["у", SoftHardType.NEUTRAL],

    ["а", SoftHardType.HARD_STRONG],
    ["о", SoftHardType.HARD_STRONG],
    ["ұ", SoftHardType.HARD_STRONG],
    ["ы", SoftHardType.HARD_STRONG],
    ["я", SoftHardType.HARD_STRONG],
]);

function wordIsSoft(raw: string): boolean {
    const w = raw.toLowerCase();
    if (HARD_SOFT_EXCEPTIONS.has(w)) {
        return HARD_SOFT_EXCEPTIONS.get(w);
    }
    for (let i = w.length - 1; i >= 0; --i) {
        let c = w[i];
        const vtype = VOWELS_BY_SOFT_HARD.get(c);
        if (vtype == undefined) {
            continue;
        }
        if (vtype == SoftHardType.SOFT_STRONG) {
            return true;
        }
        if (vtype == SoftHardType.SOFT_WEAK) {
            return true;
        }
        if (vtype == SoftHardType.HARD_STRONG) {
            return false;
        }
    }
    return false;
}

function fixBgBigrams(w: string): string {
    return (w
        .replace("бп", "пп")
        .replace("гп", "кп")
        .replace("ғп", "қп")
    );
}

function fixXkBigrams(w: string): string {
    return (w
        .replace("бқ", "пқ")
        .replace("бк", "пк")
        .replace("гк", "кк")
        .replace("ғқ", "ққ")
    );
}

function fixShortIBigrams(w: string): string {
    return (w
        .replace("йа", "я")
        .replace("ій", "и")
        .replace("ый", "и")
    );
}

function fixGgbInPastBase(c: string): string {
    if (c == 'г') {
        return 'к';
    }
    if ( c == 'ғ') {
        return 'қ';
    }
    if (c == 'б') {
        return 'п';
    }
    return c;
}
