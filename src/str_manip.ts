/* generic string manipulations that are agnostic to language/grammar details */

function getLastItem(s: string): string {
    return s[s.length - 1];
}

function getLastItemLowered(s: string): string {
    return getLastItem(s).toLowerCase();
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

function chopLast(s: string, k: number): string {
    const n = s.length;
    if (n > k) {
        return s.slice(0, n - k);
    }
    return "";
}

function replaceLast(s: string, replacement: string): string {
    const n = s.length;
    if (n == 0) {
        return replacement;
    }
    return `${s.slice(0, n - 1)}${replacement}`;
}

function replaceFirst(s: string, replacement: string): string {
    const n = s.length;
    if (n == 0) {
        return replacement;
    }
    return `${replacement}${s.slice(1, n)}`;
}

function getLastWord(s: string): string {
    let space = s.lastIndexOf(" ");
    if (space == -1) {
        return s;
    }
    return s.slice(space + 1);
}