/* generic string manipulations that are agnostic to language/grammar details */

function getLastItem(s: string): string {
    return s[s.length - 1];
}

function checkCharPresence(c: string, target: string): boolean {
    for (let i = 0; i < target.length; i++) {
        if (target[i] == c) {
            return true;
        }
    }
    return false;
}

function checkStringInList(s: string, targets: string[]): boolean {
    for (let i = 0; i < targets.length; i++) {
        if (targets[i] == s) {
            return true;
        }
    }
    return false;
}