function vowel(c: string): boolean {
    return check_char_presence(c, VOWELS);
}

function is_soft_vowel(c: string): boolean {
    return check_char_presence(c, SOFT_VOWELS);
}

function word_is_soft(w: string): boolean {
    for (let i = w.length - 1; i >= 0; --i) {
        let c = w[i];
        if (vowel(c)) {
            return is_soft_vowel(c);
        }
    }
    return false;
}