const BASE_REPLACEMENT_PKKH = new Map([
    ["п", "б"],
    ["к", "г"],
    ["қ", "ғ"],
]);

enum DropVowelType {
    Regular,
    DropLast,
    OptionallyDropLast,
};

enum Septik {
    Atau,
    Shygys,
    Jatys,
    Barys,
    Ilik,
    Tabys,
    Komektes,
};

const SEPTIKS: Septik[] = [Septik.Atau, Septik.Shygys, Septik.Jatys, Septik.Barys, Septik.Ilik, Septik.Tabys, Septik.Komektes];

const DROP_LAST_VOWEL_NOUNS = new Set([
    "әріп",
    "бөрік",
    "ғұрып",
    "дәріп",
    "ерік",
    "ерін",
    "зауық",
    "кейіп",
    "қаріп",
    "қауіп",
    "құлық",
    "құлып",
    "мойын",
    "мүлік",
    "мұрын",
    "орын",
    "парық",
    "сиық",
    "сұрық",
    "халық",
    "шырық",
    "ырық",
]);

const OPTIONALLY_DROP_LAST_VOWEL_NOUNS = new Set([
    "ауыз",
    "дауыс",
    "көрік",
    "қалып",
    "қарын",
    "қойын",
    "нарық",
    "тұрық",
]);

const FORCED_SOFT_NOUNS = new Set([
    "коньки",
    "туфли",
]);