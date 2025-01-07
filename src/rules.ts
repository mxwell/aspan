const VOWELS = "аәеиоөуүұыіэюя";
const VOWELS_EXCEPT_U_I = "аәеоөүұыіэюя";
const VOWELS_GROUP1 = "ию";
const SOFT_VOWELS = "әеөүі";
const HARD_VOWELS = "аоұы";
const CONS_GROUP1 = "руйл";
const CONS_GROUP2 = "жзмнң";
const CONS_GROUP3 = "жз";
const CONS_GROUP4 = "кқпстфхцчшщ";  // қатаң дыбыстар from https://qazaq-til-asar.livejournal.com/2594.html
const CONS_GROUP5 = "бвгғд";
const CONS_GROUP6 = "мнң";
const CONS_GROUP7 = "руй";
const CONS_GROUP8 = "жзлмнң";
const CONS_GROUP1_2 = CONS_GROUP1 + CONS_GROUP2;
const CONS_GROUP1_3 = CONS_GROUP1 + CONS_GROUP3;
const CONS_GROUP1_6 = CONS_GROUP1 + CONS_GROUP6;

const HARD_SOFT_EXCEPTIONS = new Map([
    // false - hard, true - soft

    // verbs
    ["ағжию", false],
    ["ақсию", false],
    ["ақшию", false],
    ["аңқию", false],
    ["апсию", false],
    ["арбию", false],
    ["арсию", false],
    ["аусию", false],
    ["бағбию", false],
    ["бағжию", false],
    ["бажбию", false],
    ["бақшию", false],
    ["балпию", false],
    ["балшию", false],
    ["барбию", false],
    ["баржию", false],
    ["бартию", false],
    ["баттию", false],
    ["божбию", false],
    ["болбию", false],
    ["болпию", false],
    ["борбию", false],
    ["боржию", false],
    ["борпию", false],
    ["борсию", false],
    ["бортию", false],
    ["бұқию", false],
    ["бұқшию", false],
    ["бұлтию", false],
    ["бұртию", false],
    ["былқию", false],
    ["былпию", false],
    ["былшию", false],
    ["бырбию", false],
    ["быржию", false],
    ["бырсию", false],
    ["быртию", false],
    ["быттию", false],
    ["далдию", false],
    ["дарбию", false],
    ["дардию", false],
    ["дорбию", false],
    ["дырдию", false],
    ["жалпию", false],
    ["жампию", false],
    ["жарбию", false],
    ["жию", false],
    ["жылмию", false],
    ["жылтию", false],
    ["жымию", false],
    ["жымпию", false],
    ["жымсию", false],
    ["жыртию", false],
    ["қаздию", false],
    ["қайқию", false],
    ["қаймию", false],
    ["қақию", false],
    ["қақшию", false],
    ["қалбию", false],
    ["қалқию", false],
    ["қалтию", false],
    ["қалшию", false],
    ["қампию", false],
    ["қаңқию", false],
    ["қаудию", false],
    ["қаужию", false],
    ["қауқию", false],
    ["қаупию", false],
    ["қиқию", false],
    ["қитию", false],
    ["қию", false],
    ["қоқию", false],
    ["қомпию", false],
    ["қонжию", false],
    ["қоңқию", false],
    ["қорбию", false],
    ["қоржию", false],
    ["қушию", false],
    ["құдию", false],
    ["құнжию", false],
    ["құнтию", false],
    ["құржию", false],
    ["қыдию", false],
    ["қылжию", false],
    ["қылмию", false],
    ["қылтию", false],
    ["қыржию", false],
    ["қыртию", false],
    ["лықию", false],
    ["маңқию", false],
    ["миқию", false],
    ["монтию", false],
    ["мықию", false],
    ["мықшию", false],
    ["мыржию", false],
    ["оқшию", false],
    ["сақию", false],
    ["сақсию", false],
    ["саңқию", false],
    ["сапсию", false],
    ["сидию", false],
    ["сойдию", false],
    ["соқию", false],
    ["солпию", false],
    ["сомпию", false],
    ["сопию", false],
    ["состию", false],
    ["сұстию", false],
    ["сықию", false],
    ["сықсию", false],
    ["сылқию", false],
    ["сымпию", false],
    ["сыптию", false],
    ["сырию", false],
    ["тайқию", false],
    ["тайпию", false],
    ["талпию", false],
    ["талтию", false],
    ["таңқию", false],
    ["тарбию", false],
    ["тарпию", false],
    ["тойтию", false],
    ["томпию", false],
    ["тоңқию", false],
    ["торсию", false],
    ["тортию", false],
    ["тостию", false],
    ["тотию", false],
    ["тұғжию", false],
    ["тұқию", false],
    ["тұқшию", false],
    ["тұштию", false],
    ["тылтию", false],
    ["тымпию", false],
    ["тыңқию", false],
    ["тырбию", false],
    ["тыржию", false],
    ["тырию", false],
    ["тырқию", false],
    ["тырсию", false],
    ["тыртию", false],
    ["шақшию", false],
    ["шалжию", false],
    ["шалқию", false],
    ["шанжию", false],
    ["шаңқию", false],
    ["шартию", false],
    ["шойқию", false],
    ["шоқию", false],
    ["шоқшию", false],
    ["шолжию", false],
    ["шолтию", false],
    ["шоңқию", false],
    ["шұқию", false],
    ["шұқшию", false],
    ["шұнтию", false],
    ["шықию", false],
    ["шылқию", false],
    ["ыздию", false],
    ["ыңқию", false],
    ["ыржию", false],
    ["ырсию", false],
    // nouns
    ["ми", false],
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

const VERB_PERS_AFFIXES2: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
    First: {
        Singular: ["м", "м"],
        Plural: ["қ", "к"],
    },
    Second: {
        Singular: ["ң", "ң"],
        Plural: ["ңдар", "ңдер"],
    },
    SecondPolite: {
        Singular: ["ңыз", "ңіз"],
        Plural: ["ңыздар", "ңіздер"],
    },
    Third: {
        Singular: ["", ""],
        Plural: ["", ""]
    }
};

const VERB_WANT_PERS_AFFIXES: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
    First: {
        Singular: ["м", "м"],
        Plural: ["мыз", "міз"],
    },
    Second: {
        Singular: ["ң", "ң"],
        Plural: ["ларың", "лерің"],
    },
    SecondPolite: {
        Singular: ["ңыз", "ңіз"],
        Plural: ["ларыңыз", "леріңіз"],
    },
    Third: {
        Singular: ["сы", "сі"],
        Plural: ["лары", "лері"]
    }
};

const FIRST_PERS_AFFIXES1: Record<GrammarNumber, Record<PersAffix1LetterGroup, string[]>> = {
    Singular: {
        PersAffix1DefaultGroup: ["мын", "мін"],
        PersAffix1GzGroup: ["бын", "бін"],
        PersAffix1MnGroup: ["мын", "мін"],
        PersAffixUnvoicedGroup: ["пын", "пін"],
    },
    Plural: {
        PersAffix1DefaultGroup: ["мыз", "міз"],
        PersAffix1GzGroup: ["быз", "біз"],
        PersAffix1MnGroup: ["быз", "біз"],
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

const THIRD_PERS_AFFIXES3: Record<GrammarNumber, string[]> = {
    Singular: ["ты", "ті"],
    Plural: ["ты", "ті"],
};

const IMPERATIVE_AFFIXES: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
    First: {
        Singular: ["йын", "йін"],
        Plural:   ["йық", "йік"],
    },
    Second: {
        Singular: ["", ""],
        Plural:   ["ңдар", "ңдер"],
    },
    SecondPolite: {
        Singular: ["ңыз", "ңіз"],
        Plural:   ["ңыздар", "ңіздер"],
    },
    Third: {
        Singular: ["сын", "сін"],
        Plural:   ["сын", "сін"],
    }
};

const AE = ["а", "е"];
const YI = ["ы", "і"];

const MAME = ["ма", "ме"];
const BABE = ["ба", "бе"];
const PAPE = ["па", "пе"];

/* These are used in negative forms of colloqiual present continuous */
const CLQ_MAME = ["ма", "ми"];
const CLQ_BABE = ["ба", "би"];
const CLQ_PAPE = ["па", "пи"];

const MAKMEK = ["мақ", "мек"];
const BAKBEK = ["бақ", "бек"];
const PAKPEK = ["пақ", "пек"];

const GANGEN = ["ған", "ген"];
const KANKEN = ["қан", "кен"];

const GALYGELI = ["ғалы", "гелі"];
const KALYKELI = ["қалы", "келі"];

const GYGI = ["ғы", "гі"];
const KYKI = ["қы", "кі"];

const DYDI = ["ды", "ді"];
const TYTI = ["ты", "ті"];

const YPIP = ["ып", "іп"];
const AFFIX_ATYR = ["атыр", "ятыр"];
const AFFIX_AT = ["ат", "ят"];

const SASE = ["са", "се"];

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

const LAT2021_PRONOUN_BY_PERSON_NUMBER: Record<GrammarPerson, Record<GrammarNumber, String>> = {
    First: {
        Singular: "men",
        Plural: "bız"
    },
    Second: {
        Singular: "sen",
        Plural: "sender"
    },
    SecondPolite: {
        Singular: "Sız",
        Plural: "Sızder"
    },
    Third: {
        Singular: "ol",
        Plural: "olar"
    }
};

const POSSESSIVE_BY_PERSON_NUMBER: Record<GrammarPerson, Record<GrammarNumber, String>> = {
    First: {
        Singular: "менің",
        Plural: "біздің"
    },
    Second: {
        Singular: "сенің",
        Plural: "сендердің"
    },
    SecondPolite: {
        Singular: "Сіздің",
        Plural: "Сіздердің"
    },
    Third: {
        Singular: "оның",
        Plural: "олардың"
    }
};

const LAT2021_POSSESSIVE_BY_PERSON_NUMBER: Record<GrammarPerson, Record<GrammarNumber, String>> = {
    First: {
        Singular: "menıñ",
        Plural: "bızdıñ"
    },
    Second: {
        Singular: "senıñ",
        Plural: "senderdıñ"
    },
    SecondPolite: {
        Singular: "Sızdıñ",
        Plural: "Sızderdıñ"
    },
    Third: {
        Singular: "onyñ",
        Plural: "olardyñ"
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

/* verb to [regular meanings, irregular meanings] */
const OPT_EXCEPT_VERB_MEANINGS = new Map([
    ["ашу", [["открывать", "выявлять"], ["киснуть", "сквашиваться"]]],
    ["еру", [["следовать", "внимать"], ["таять", "растворяться"]]],
    ["жану", [["гореть", "пылать"], ["точить", "править", "оттачивать"]]],
    ["жару", [["колоть", "разрывать"], ["быть обеспеченным"]]],
    ["жуу", [["мыть", "обмывать"], ["быть близким"]]],
    ["ию", [["гнуть", "сгибать"], ["спускать молоко", "раздобриться"]]],
    ["қабу", [["хватать", "ловить"], ["стегать", "простёгивать"]]],
    ["құру", [["строить", "устанавливать"], ["вымирать", "пропадать"]]],
    ["пысу", [["пугаться", "страшиться"], ["крепнуть", "скручиваться"]]],
    ["сасу", [["суетиться", "теряться"], ["вонять", "протухать"]]],
    ["тану", [["отказываться", "отрекаться"], ["узнавать", "знакомиться"]]],
    ["тату", [["отведывать", "есть", "испытывать"], ["приобретать вкус", "заслуживать"]]],
    ["ысу", [["тереть", "натирать"], ["нагреваться", "теплеть"]]],
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

const VERB_PRESENT_CONT_EXCEPTION_A_SET = new Set([
    "бару",
    "апару",
]);

const VERB_PRESENT_CONT_EXCEPTION_E_SET = new Set([
    "келу",
    "әкелу",
]);

const VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED = "жату";

/* TODO are there more of them? */
const VERB_PRESENT_CONT_EXCEPTION_U_SET = new Set([
    "жабу",
    "қабу",
    "кебу",
    "себу",
    "тебу",
    "табу",
    "шабу",
]);

const VERB_EXCEPTION_ADD_VOWEL_MAP = new Map([
    ["қорқу", "қорық"],
    ["қырқу", "қырық"],
    ["ірку", "ірік"],
    ["бүрку", "бүрік"],
]);

const VERB_LAST_NEGATIVE_CONVERSION = new Map([
    ["б", "п"],
    ["г", "к"],
    ["ғ", "қ"],
]);