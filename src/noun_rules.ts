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
    Atau = "Atau",
    Ilik = "Ilik",
    Barys = "Barys",
    Tabys = "Tabys",
    Jatys = "Jatys",
    Shygys = "Shygys",
    Komektes = "Komektes",
};

const SEPTIKS: Septik[] = [Septik.Atau, Septik.Ilik, Septik.Barys, Septik.Tabys, Septik.Jatys, Septik.Shygys, Septik.Komektes];

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

const OPTIONALLY_DROP_LAST_VOWEL_NOUNS = new Map<string, string[]>([
    ["ауыз", ["рот", "рот"]],
    ["дауыс", ["звук", "звук"]],
    ["көрік", ["красота", "кузнечный мех"]],
    ["қалып", ["состояние", "колодка, шаблон"]],
    ["қарын", ["желудок", "желудок"]],
    ["қойын", ["пазуха", "пазуха"]],
    ["нарық", ["расценка, тариф", "расценка, тариф"]],
    ["тұрық", ["длина", "местожительство"]],
]);