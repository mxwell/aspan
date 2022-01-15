function vowel(c: string): boolean {
    return checkCharPresence(c, VOWELS);
}

function isSoftVowel(c: string): boolean {
    return checkCharPresence(c, SOFT_VOWELS);
}

function wordIsSoft(w: string): boolean {
    for (let i = w.length - 1; i >= 0; --i) {
        let c = w[i];
        if (vowel(c)) {
            return isSoftVowel(c);
        }
    }
    return false;
}

function fixBigrams(w: string): string {
    return (w
        .replace("бп", "пп")
        .replace("гп", "кп")
    );
}