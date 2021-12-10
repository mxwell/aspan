/* generic string manipulations that are agnostic to language/grammar details */

function getLastItem(s: string): string {
    return s[s.length - 1];
}

function check_char_presence(c: string, target: string): boolean {
    for (let i = 0; i < target.length; i++) {
        if (target[i] == c) {
            return true;
        }
    }
    return false;
}

function check_string_in_list(s: string, targets: string[]): boolean {
    for (let i = 0; i < targets.length; i++) {
        if (targets[i] == s) {
            return true;
        }
    }
    return false;
}