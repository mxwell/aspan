const LARLER = ["лар", "лер"];
const TARTER = ["тар", "тер"];
const DARDER = ["дар", "дер"];

const DANDEN = ["дан", "ден"];
const TANTEN = ["тан", "тен"];
const NANNEN = ["нан", "нен"];

const DADE = ["да", "де"];
const TATE = ["та", "те"];
const NDANDE = ["нда", "нде"];

const DAGYDEGI = ["дағы", "дегі"];
const TAGYTEGI = ["тағы", "тегі"];
const NDAGYNDEGI = ["ндағы", "ндегі"];

const GAGE = ["ға", "ге"];
const KAKE = ["қа", "ке"];
const NANE = ["на", "не"];

const DYNGDING = ["дың", "дің"];
const TYNGTING = ["тың", "тің"];
const NYNGNING = ["ның", "нің"];

const NYNI = ["ны", "ні"];

const NOUN_POSSESSIVE_AFFIXES: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
    First: {
        Singular: ["м", "м"],
        Plural: ["мыз", "міз"],
    },
    Second: {
        Singular: ["ң", "ң"],
        Plural: ["ң", "ң"],
    },
    SecondPolite: {
        Singular: ["ңыз", "ңіз"],
        Plural: ["ңыз", "ңіз"],
    },
    Third: {
        Singular: ["ы", "і"],
        Plural: ["ы", "і"],
    }
};