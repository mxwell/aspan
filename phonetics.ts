function vowel(c: string): boolean {
    return checkCharPresence(c, VOWELS);
}

function genuineVowel(c: string): boolean {
    return checkCharPresence(c, VOWELS_EXCEPT_U);
}

function isSoftVowel(c: string): boolean {
    return checkCharPresence(c, SOFT_VOWELS);
}

function isHardVowel(c: string): boolean {
    return checkCharPresence(c, HARD_VOWELS);
}

function wordIsSoft(w: string): boolean {
    for (let i = w.length - 1; i >= 0; --i) {
        let c = w[i];
        if (isSoftVowel(c)) {
            return true;
        }
        if (isHardVowel(c)) {
            return false;
        }
    }
    return false;
}

// TODO better name and more cases
function fixBigrams(w: string): string {
    return (w
        .replace("бп", "пп")
        .replace("гп", "кп")
    );
}

function fixShortIBigrams(w: string): string {
    return (w
        .replace("йа", "я")
        .replace("ій", "и")
        .replace("ый", "и")
    );
}