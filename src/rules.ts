const VOWELS = "аәеиоөуүұыіэюя";
const VOWELS_EXCEPT_U_I = "аәеоөүұыіэюя";
const SOFT_VOWELS = "әеөүі";
const HARD_VOWELS = "аоұы";
const CONS_GROUP1 = "руйл";
const CONS_GROUP2 = "жзмнң";
const CONS_GROUP3 = "жз";
const CONS_GROUP4 = "кқпстфхцчшщ";  // қатаң дыбыстар from https://qazaq-til-asar.livejournal.com/2594.html
const CONS_GROUP5 = "бвгғд";

const FORCED_SOFT_VERBS = new Set([
    "кию",  // киемің is a quite frequent form
    "жиду", // https://kaz-tili.kz/gl04.htm
    "тобарсу", // https://kaz-tili.kz/gl04.htm
    "итию", // https://kaz-tili.kz/gl04.htm
]);

const VERB_PERS_AFFIXES1: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
    First: {
        Singular: ["мын", "мін"],
        Plural: ["мыз", "міз"],
    },
    Second: {
        Singular: ["сың", "сің"],
        Plural: ["сыңдар", "сіңдер"],
    },
    SecondPolite: {
        Singular: ["сыз", "сіз"],
        Plural: ["сыздар", "сіздер"],
    },
    Third: {
        Singular: ["ды", "ді"],
        Plural: ["ды", "ді"]
    }
};

const FIRST_PERS_AFFIXES1: Record<GrammarNumber, Record<PersAffix1LetterGroup, string[]>> = {
    Singular: {
        PersAffix1DefaultGroup: ["мын", "мін"],
        PersAffix1GzGroup: ["бын", "бін"],
        PersAffixUnvoicedGroup: ["пын", "пін"],
    },
    Plural: {
        PersAffix1DefaultGroup: ["мыз", "міз"],
        PersAffix1GzGroup: ["быз", "біз"],
        PersAffixUnvoicedGroup: ["пыз", "піз"]
    },
};
const SECOND_PERS_AFFIXES1: Record<GrammarNumber, string[]> = {
    Singular: ["сың", "сің"],
    Plural: ["сыңдар", "сіңдер"],
};
const SECOND_POLITE_PERS_AFFIXES1: Record<GrammarNumber, string[]> = {
    Singular: ["сыз", "сіз"],
    Plural: ["сыздар", "сіздер"],
};

const MAME = ["ма", "ме"];
const BABE = ["ба", "бе"];
const PAPE = ["па", "пе"];

const GANGEN = ["ған", "ген"];
const KANKEN = ["қан", "кен"];

const PRONOUN_BY_PERSON_NUMBER: Record<GrammarPerson, Record<GrammarNumber, String>> = {
    First: {
        Singular: "мен",
        Plural: "біз"
    },
    Second: {
        Singular: "сен",
        Plural: "сендер"
    },
    SecondPolite: {
        Singular: "Сіз",
        Plural: "Сіздер"
    },
    Third: {
        Singular: "ол",
        Plural: "олар"
    }
};

/* we should add і/ы to the base for the following: */
const VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET = new Set([
    "абыржу",
    "ағайынсу",
    "адалсу",
    "адамсу",
    "айну",
    "ақылгөйсу",
    "ақылсу",
    "ақынсу",
    "алжу",
    "аңду",
    "аңқаусу",
    "аңқу",
    "апшу",
    "арзу",
    "ару",
    "аршу",
    "астамсу",
    "атқу",
    "аунақшу",
    "ауытқу",
    "аярсу",
    "аяқсу",
    "әзілдегенсу",
    "әкімсу",
    "әсемсу",
    "әспенсу",
    "балқу",
    "балуансу",
    "батпансу",
    "батырсу",
    "батырымсу",
    "баулу",
    "баяусу",
    "бәлсу",
    "бәсеңсу",
    "бейкүнәмсу",
    "бейқамсу",
    "беку",
    "берегенсу",
    "берку",
    "болғансу",
    "боржу",
    "борсу",
    "босаңсу",
    "бөлексу",
    "бөтенсу",
    "буазу",
    "бұлқу",
    "бұлықсу",
    "быжу",
    "бықсу",
    "бықу",
    "бықырсу",
    "былқу",
    "быршу",
    "білгенсу",
    "білгірсу",
    "білгішсу",
    "білдіргенсу",
    "даму",
    "данагөйсу",
    "данасу",
    "дандайсу",
    "данышпансу",
    "даңғойсу",
    "дардайсу",
    "дарқансу",
    "дару",
    "дәнсу",
    "дәусу",
    "дегду",
    "дөңбекшу",
    "дөрекпу",
    "дүмпу",
    "дүңку",
    "ділмарсу",
    "діндарсу",
    "елегенсу",
    "елту",
    "емексу",
    "еркексу",
    "еркесу",
    "еркінсу",
    "ерсу",
    "есту",
    "есіркегенсу",
    "жағымсу",
    "жадыгөйсу",
    "жайбарақатсу",
    "жайдақсу",
    "жақынсу",
    "жалғызсу",
    "жалқаусу",
    "жалқу",
    "жану",
    "жаншу",
    "жасу",
    "жаталақшу",
    "жеку",
    "желпу",
    "жеңілгенсу",
    "жеру",
    "жиду",
    "жомартсу",
    "жору",
    "жосу",
    "жөңку",
    "жуасу",
    "жұлқу",
    "жүйтку",
    "жүнжу",
    "жыбыршу",
    "жылжу",
    "жылу",
    "жылымсу",
    "жылымшу",
    "жібу",
    "жігітсу",
    "жіпсу",
    "зеку",
    "зеңгу",
    "зырғу",
    "кеберсу",
    "кебірсу",
    "кебіртексу",
    "кедейсу",
    "кему",
    "кеңу",
    "кепсу",
    "кербезсу",
    "кергу",
    "кереметсу",
    "керсу",
    "көбеңсу",
    "көгілжу",
    "көлгірсу",
    "көлку",
    "көнсу",
    "көншу",
    "көңірсу",
    "көпсу",
    "көпіршу",
    "көсемсу",
    "күлімсу",
    "күмілжу",
    "күнсу",
    "күпсу",
    "күпу",
    "кілку",
    "кінәзсу",
    "кісімсу",
    "қабаржу",
    "қағып-сілку",
    "қағылжу",
    "қажу",
    "қаймақшу",
    "қақсу",
    "қақшу",
    "қалғу",
    "қалқу",
    "қамқорсу",
    "қамту",
    "қаңғу",
    "қаңсу",
    "қарбу",
    "қарғу",
    "қарпу",
    "қару",
    "қасаңсу",
    "қасу",
    "қобалжу",
    "қожайынсу",
    "қоқсу",
    "қоқу",
    "қоқырсу",
    "қоңылтақсу",
    "қору",
    "құбылжу",
    "құдайсу",
    "құйқылжу",
    "құлазу",
    "құрғақсу",
    "қылғу",
    "қылпу",
    "қылымсу",
    "қымқу",
    "қымту",
    "қыңсу",
    "қырпу",
    "қыршу",
    "қышу",
    "ләйлу",
    "леку",
    "лоблу",
    "лоқсу",
    "лықсу",
    "лықу",
    "лыпу",
    "малту",
    "малшу",
    "манду",
    "маңғазсу",
    "марғаусу",
    "мардамсу",
    "мәңгу",
    "менменсу",
    "меңіреусу",
    "мойынсу",
    "момақансу",
    "мұжу",
    "мүжу",
    "мүләйімсу",
    "мүлгу",
    "мүңку",
    "мүсәпірсу",
    "мығымсу",
    "мыжу",
    "мызғу",
    "мылқаусу",
    "мырзасу",
    "мытқу",
    "мыту",
    "міндетсу",
    "налу",
    "нұқу",
    "обалсу",
    "ойнақшу",
    "оқу",
    "орғу",
    "ортқу",
    "оршу",
    "өгейсу",
    "өзімсу",
    "өксу",
    "өкімсу",
    "өрбу",
    "өрекпу",
    "өрекшу",
    "өршу",
    "өсту",
    "өсіп-өрбу",
    "пақырсу",
    "палуансу",
    "паңсу",
    "пысықсу",
    "ренжу",
    "салақсу",
    "салғансу",
    "салғыртсу",
    "салқамсу",
    "салқынсу",
    "самарқаусу",
    "самсу",
    "саңғу",
    "сапсу",
    "сараңсу",
    "сарқу",
    "сарсу",
    "сару",
    "саябырсу",
    "саяқсу",
    "сәнсу",
    "сәуегейсу",
    "сенгенсу",
    "сепсу",
    "сергексу",
    "сергу",
    "серпу",
    "серімсу",
    "сету",
    "сирексу",
    "сорғу",
    "сусу",
    "суу",
    "сүңгу",
    "сылту",
    "сылу",
    "сыңсу",
    "сыпайысу",
    "сырғақсу",
    "сырғу",
    "сыру",
    "сілку",
    "тайқақсу",
    "тайқу",
    "талмаусу",
    "талықсу",
    "тамылжу",
    "танту",
    "тарпу",
    "тартқансу",
    "тасу",
    "тәкаппарсу",
    "тәлімсу",
    "тәңірсу",
    "тәуелжу",
    "тәуірсу",
    "телу",
    "тепшу",
    "терлеп-тепшу",
    "тершу",
    "тетку",
    "тобарсу",
    "тоқмейілсу",
    "тоқу",
    "толқу",
    "толықсу",
    "тоңазу",
    "тору",
    "төменсу",
    "тұшу",
    "түйткілжу",
    "түйткілсу",
    "түлежу",
    "тықыршу",
    "тыншу",
    "тыпыршу",
    "тілмарсу",
    "уылжу",
    "ұйтқу",
    "ұлу",
    "ұлықсу",
    "ұңғу",
    "үлкенсу",
    "үңгу",
    "үстемсу",
    "үсу",
    "шалқу",
    "шанду",
    "шаншу",
    "шапшу",
    "шарпу",
    "шеку",
    "шешенсу",
    "шоқу",
    "шоршу",
    "шошу",
    "шөжу",
    "шөку",
    "шөпшу",
    "шұқу",
    "шұлғу",
    "шүйгу",
    "шүленсу",
    "шыжу",
    "шылқу",
    "шымшу",
    "шыпшу",
    "шырпу",
    "шіру",
    "ыбылжу",
    "ыбырсу",
    "ызғу",
    "ыңырсу",
    "ырғу",
    "ыршу",
    "ытқу",
    "ілбу",
    "іру",
]);

const VERB_PRESENT_TRANSITIVE_OPTIONAL_EXCEPTIONS_SET = new Set([
    "ашу",
    "еру",
    "жару",
    "жуу",
    "қабу",
    "құру",
    "пысу",
    "сасу",
    "тану",
    "тату",
    "ысу",
]);

const VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX = ["ы", "і"];

const VERB_PRESENT_TRANSITIVE_EXCEPTIONS2_SET = new Set([
    "баю",
    "кею",
    "қаю",
    "мою",
    "ұю",
]);

const VERB_PRESENT_CONT_BASE_MAP: Map<string, string> = new Map([
    ["тұру", "тұр"],
    ["жүру", "жүр"],
    ["отыру", "отыр"],
    ["жату", "жатыр"],
]);