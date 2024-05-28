function vowel(c: string): boolean {
    return checkCharPresence(c, VOWELS);
}

function genuineVowel(c: string): boolean {
    return checkCharPresence(c, VOWELS_EXCEPT_U_I);
}

function isSoftVowel(c: string): boolean {
    return checkCharPresence(c, SOFT_VOWELS);
}

function isHardVowel(c: string): boolean {
    return checkCharPresence(c, HARD_VOWELS);
}

function wordIsSoft(w: string): boolean {
    for (let i = w.length - 1; i >= 0; --i) {
        let c = w[i].toLowerCase();
        if (isSoftVowel(c)) {
            return true;
        }
        if (isHardVowel(c)) {
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
