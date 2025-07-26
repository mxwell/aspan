package com.khairulin.kazakhverb.grammar

object Rules {
    val VOWELS = setOf(
        'а',
        'ә',
        'е',
        'и',
        'о',
        'ө',
        'у',
        'ү',
        'ұ',
        'ы',
        'і',
        'э',
        'ю',
        'я'
    )
    val VOWELS_EXCEPT_U_I = setOf(
        'а',
        'ә',
        'е',
        'о',
        'ө',
        'ү',
        'ұ',
        'ы',
        'і',
        'э',
        'ю',
        'я'
    )

    val VOWELS_GROUP1 = setOf(
        'и',
        'ю'
    )

    val CONS_GROUP1 = setOf(
        'р',
        'у',
        'й',
        'л'
    )
    val CONS_GROUP2 = setOf(
        'ж',
        'з',
        'м',
        'н',
        'ң'
    )
    val CONS_GROUP3 = setOf(
        'ж',
        'з'
    )
    val CONS_GROUP4 = setOf(
        'к',
        'қ',
        'п',
        'с',
        'т',
        'ф',
        'х',
        'ц',
        'ч',
        'ш',
        'щ'
    )
    val CONS_GROUP5 = setOf(
        'б',
        'в',
        'г',
        'ғ',
        'д'
    )
    val CONS_GROUP6 = setOf(
        'м',
        'н',
        'ң'
    )
    val CONS_GROUP7 = setOf(
        'р',
        'у',
        'й'
    )
    val CONS_GROUP8 = setOf(
        'ж',
        'з',
        'л',
        'м',
        'н',
        'ң'
    )
    val CONS_GROUP1_2 = CONS_GROUP1.union(CONS_GROUP2)
    val CONS_GROUP1_3 = CONS_GROUP1.union(CONS_GROUP3)
    val CONS_GROUP1_6 = CONS_GROUP1.union(CONS_GROUP6)

    val VERB_PERS_AFFIXES1: Map<GrammarPerson, Map<GrammarNumber, List<String>>> = mapOf(
        GrammarPerson.First to mapOf(
            GrammarNumber.Singular to listOf("мын", "мін"),
            GrammarNumber.Plural to listOf("мыз", "міз")
        ),
        GrammarPerson.Second to mapOf(
            GrammarNumber.Singular to listOf("сың", "сің"),
            GrammarNumber.Plural to listOf("сыңдар", "сіңдер")
        ),
        GrammarPerson.SecondPolite to mapOf(
            GrammarNumber.Singular to listOf("сыз", "сіз"),
            GrammarNumber.Plural to listOf("сыздар", "сіздер")
        ),
        GrammarPerson.Third to mapOf(
            GrammarNumber.Singular to listOf("ды", "ді"),
            GrammarNumber.Plural to listOf("ды", "ді")
        )
    )

    val VERB_PERS_AFFIXES2: Map<GrammarPerson, Map<GrammarNumber, List<String>>> = mapOf(
        GrammarPerson.First to mapOf(
            GrammarNumber.Singular to listOf("м", "м"),
            GrammarNumber.Plural to listOf("қ", "к")
        ),
        GrammarPerson.Second to mapOf(
            GrammarNumber.Singular to listOf("ң", "ң"),
            GrammarNumber.Plural to listOf("ңдар", "ңдер")
        ),
        GrammarPerson.SecondPolite to mapOf(
            GrammarNumber.Singular to listOf("ңыз", "ңіз"),
            GrammarNumber.Plural to listOf("ңыздар", "ңіздер")
        ),
        GrammarPerson.Third to mapOf(
            GrammarNumber.Singular to listOf("", ""),
            GrammarNumber.Plural to listOf("", "")
        )
    )

    val VERB_WANT_PERS_AFFIXES: Map<GrammarPerson, Map<GrammarNumber, List<String>>> = mapOf(
        GrammarPerson.First to mapOf(
            GrammarNumber.Singular to listOf("м", "м"),
            GrammarNumber.Plural to listOf("мыз", "міз")
        ),
        GrammarPerson.Second to mapOf(
            GrammarNumber.Singular to listOf("ң", "ң"),
            GrammarNumber.Plural to listOf("ларың", "лерің")
        ),
        GrammarPerson.SecondPolite to mapOf(
            GrammarNumber.Singular to listOf("ңыз", "ңіз"),
            GrammarNumber.Plural to listOf("ларыңыз", "леріңіз")
        ),
        GrammarPerson.Third to mapOf(
            GrammarNumber.Singular to listOf("сы", "сі"),
            GrammarNumber.Plural to listOf("лары", "лері")
        )
    )

    val FIRST_PERS_AFFIXES1: Map<GrammarNumber, Map<PersAffix1LetterGroup, List<String>>> = mapOf(
        GrammarNumber.Singular to mapOf(
            PersAffix1LetterGroup.PersAffix1DefaultGroup to listOf("мын", "мін"),
            PersAffix1LetterGroup.PersAffix1GzGroup to listOf("бын", "бін"),
            PersAffix1LetterGroup.PersAffix1MnGroup to listOf("мын", "мін"),
            PersAffix1LetterGroup.PersAffixUnvoicedGroup to listOf("пын", "пін")
        ),
        GrammarNumber.Plural to mapOf(
            PersAffix1LetterGroup.PersAffix1DefaultGroup to listOf("мыз", "міз"),
            PersAffix1LetterGroup.PersAffix1GzGroup to listOf("быз", "біз"),
            PersAffix1LetterGroup.PersAffix1MnGroup to listOf("быз", "біз"),
            PersAffix1LetterGroup.PersAffixUnvoicedGroup to listOf("пыз", "піз")
        )
    )

    val SECOND_PERS_AFFIXES1: Map<GrammarNumber, List<String>> = mapOf(
        GrammarNumber.Singular to listOf("сың", "сің"),
        GrammarNumber.Plural to listOf("сыңдар", "сіңдер")
    )

    val SECOND_POLITE_PERS_AFFIXES1: Map<GrammarNumber, List<String>> = mapOf(
        GrammarNumber.Singular to listOf("сыз", "сіз"),
        GrammarNumber.Plural to listOf("сыздар", "сіздер")
    )

    val THIRD_PERS_AFFIXES3: Map<GrammarNumber, List<String>> = mapOf(
        GrammarNumber.Singular to listOf("ты", "ті"),
        GrammarNumber.Plural to listOf("ты", "ті"),
    )

    val IMPERATIVE_AFFIXES: Map<GrammarPerson, Map<GrammarNumber, List<String>>> = mapOf(
        GrammarPerson.First to mapOf(
            GrammarNumber.Singular to listOf("йын", "йін"),
            GrammarNumber.Plural to listOf("йық", "йік")
        ),
        GrammarPerson.Second to mapOf(
            GrammarNumber.Singular to listOf("", ""),
            GrammarNumber.Plural to listOf("ңдар", "ңдер")
        ),
        GrammarPerson.SecondPolite to mapOf(
            GrammarNumber.Singular to listOf("ңыз", "ңіз"),
            GrammarNumber.Plural to listOf("ңыздар", "ңіздер")
        ),
        GrammarPerson.Third to mapOf(
            GrammarNumber.Singular to listOf("сын", "сін"),
            GrammarNumber.Plural to listOf("сын", "сін")
        )
    )

    val NOUN_POSSESSIVE_AFFIXES: Map<GrammarPerson, Map<GrammarNumber, List<String>>> = mapOf(
        GrammarPerson.First to mapOf(
            GrammarNumber.Singular to listOf("м", "м"),
            GrammarNumber.Plural to listOf("мыз", "міз"),
        ),
        GrammarPerson.Second to mapOf(
            GrammarNumber.Singular to listOf("ң", "ң"),
            GrammarNumber.Plural to listOf("ң", "ң"),
        ),
        GrammarPerson.SecondPolite to mapOf(
            GrammarNumber.Singular to listOf("ңыз", "ңіз"),
            GrammarNumber.Plural to listOf("ңыз", "ңіз"),
        ),
        GrammarPerson.Third to mapOf(
            GrammarNumber.Singular to listOf("ы", "і"),
            GrammarNumber.Plural to listOf("ы", "і"),
        ),
    )

    val AE = listOf("а", "е")
    val YI = listOf("ы", "і")
    val MAME = listOf("ма", "ме")
    val BABE = listOf("ба", "бе")
    val PAPE = listOf("па", "пе")
    val GANGEN = listOf("ған", "ген")
    val KANKEN = listOf("қан", "кен")
    val GYGI = listOf("ғы", "гі")
    val KYKI = listOf("қы", "кі")
    val DYDI = listOf("ды", "ді")
    val TYTI = listOf("ты", "ті")
    val YPIP = listOf("ып", "іп")
    val MAKMEK = listOf("мақ", "мек")
    val BAKBEK = listOf("бақ", "бек")
    val PAKPEK = listOf("пақ", "пек")
    val SASE = listOf("са", "се")
    val YTYNYTIN = listOf("йтын", "йтін")
    val ATYNETYN = listOf("атын", "етін")
    val YATYN = "ятын"
    val USHYUSHI = listOf("ушы", "уші")

    val LARLER = listOf("лар", "лер")
    val TARTER = listOf("тар", "тер")
    val DARDER = listOf("дар", "дер")

    val DANDEN = listOf("дан", "ден")
    val TANTEN = listOf("тан", "тен")
    val NANNEN = listOf("нан", "нен")

    val DADE = listOf("да", "де")
    val TATE = listOf("та", "те")
    val NDANDE = listOf("нда", "нде")

    val NANE = listOf("на", "не")
    val GAGE = listOf("ға", "ге")
    val KAKE = listOf("қа", "ке")

    val DYNGDING = listOf("дың", "дің")
    val TYNGTING = listOf("тың", "тің")
    val NYNGNING = listOf("ның", "нің")

    val NYNI = listOf("ны", "ні")

    val RAKREK = listOf("рақ", "рек")
    val YRAKIREK = listOf("ырақ", "ірек")
    val LAULEU = listOf("лау", "леу")
    val DAUDEU = listOf("дау", "деу")
    val TAUTEU = listOf("тау", "теу")

    val OPT_EXCEPT_VERB_MEANINGS: Map<String, List<List<String>>> = mapOf(
        "ашу" to listOf(listOf("открывать", "выявлять"), listOf("киснуть", "сквашиваться")),
        "еру" to listOf(listOf("следовать", "внимать"), listOf("таять", "растворяться")),
        "жану" to listOf(listOf("гореть", "пылать"), listOf("точить", "править", "оттачивать")),
        "жару" to listOf(listOf("колоть", "разрывать"), listOf("быть обеспеченным")),
        "жуу" to listOf(listOf("мыть", "обмывать"), listOf("быть близким")),
        "ию" to listOf(listOf("гнуть", "сгибать"), listOf("спускать молоко", "раздобриться")),
        "қабу" to listOf(listOf("хватать", "ловить"), listOf("стегать", "простёгивать")),
        "құру" to listOf(listOf("строить", "устанавливать"), listOf("вымирать", "пропадать")),
        "пысу" to listOf(listOf("пугаться", "страшиться"), listOf("крепнуть", "скручиваться")),
        "сасу" to listOf(listOf("суетиться", "теряться"), listOf("вонять", "протухать")),
        "тану" to listOf(listOf("отказываться", "отрекаться"), listOf("узнавать", "знакомиться")),
        "тату" to listOf(listOf("отведывать", "есть", "испытывать"), listOf("приобретать вкус", "заслуживать")),
        "ысу" to listOf(listOf("тереть", "натирать"), listOf("нагреваться", "теплеть"))
    )

    val VERB_PRESENT_TRANSITIVE_EXCEPTIONS_BASE_SUFFIX = listOf("ы", "і")

    val VERB_PRESENT_TRANSITIVE_EXCEPTIONS2_SET = setOf(
        "баю",
        "кею",
        "қаю",
        "мою",
        "ұю"
    )

    val VERB_PRESENT_CONT_BASE_MAP = mapOf(
        "тұру" to "тұр",
        "жүру" to "жүр",
        "отыру" to "отыр",
        "жату" to "жатыр"
    )

    val VERB_PRESENT_CONT_EXCEPTION_A_SET = setOf(
        "бару",
        "апару"
    )

    val VERB_PRESENT_CONT_EXCEPTION_E_SET = setOf(
        "келу",
        "әкелу"
    )

    val VERB_PRESENT_CONT_EXCEPTION_AE_AUX_ENABLED = "жату"

    val VERB_PRESENT_CONT_EXCEPTION_U_SET = setOf(
        "жабу",
        "қабу",
        "кебу",
        "себу",
        "тебу",
        "табу",
        "шабу"
    )

    val VERB_EXCEPTION_ADD_VOWEL_MAP = mapOf(
        "қорқу" to "қорық",
        "қырқу" to "қырық",
        "ірку" to "ірік",
        "бүрку" to "бүрік"
    )

    val VERB_LAST_NEGATIVE_CONVERSION = mapOf(
        'б' to 'п',
        'г' to 'к',
        'ғ' to 'қ'
    )

    val HARD_SOFT_EXCEPTIONS: Map<String, Boolean> = mapOf(
        // false - hard, true - soft

        // verbs
        "ағжию" to false,
        "ақсию" to false,
        "ақшию" to false,
        "аңқию" to false,
        "апсию" to false,
        "арбию" to false,
        "арсию" to false,
        "аусию" to false,
        "бағбию" to false,
        "бағжию" to false,
        "бажбию" to false,
        "бақшию" to false,
        "балпию" to false,
        "балшию" to false,
        "барбию" to false,
        "баржию" to false,
        "бартию" to false,
        "баттию" to false,
        "божбию" to false,
        "болбию" to false,
        "болпию" to false,
        "борбию" to false,
        "боржию" to false,
        "борпию" to false,
        "борсию" to false,
        "бортию" to false,
        "бұқию" to false,
        "бұқшию" to false,
        "бұлтию" to false,
        "бұртию" to false,
        "былқию" to false,
        "былпию" to false,
        "былшию" to false,
        "бырбию" to false,
        "быржию" to false,
        "бырсию" to false,
        "быртию" to false,
        "быттию" to false,
        "далдию" to false,
        "дарбию" to false,
        "дардию" to false,
        "дорбию" to false,
        "дырдию" to false,
        "жалпию" to false,
        "жампию" to false,
        "жарбию" to false,
        "жию" to false,
        "жылмию" to false,
        "жылтию" to false,
        "жымию" to false,
        "жымпию" to false,
        "жымсию" to false,
        "жыртию" to false,
        "қаздию" to false,
        "қайқию" to false,
        "қаймию" to false,
        "қақию" to false,
        "қақшию" to false,
        "қалбию" to false,
        "қалқию" to false,
        "қалтию" to false,
        "қалшию" to false,
        "қампию" to false,
        "қаңқию" to false,
        "қаудию" to false,
        "қаужию" to false,
        "қауқию" to false,
        "қаупию" to false,
        "қиқию" to false,
        "қитию" to false,
        "қию" to false,
        "қоқию" to false,
        "қомпию" to false,
        "қонжию" to false,
        "қоңқию" to false,
        "қорбию" to false,
        "қоржию" to false,
        "қушию" to false,
        "құдию" to false,
        "құнжию" to false,
        "құнтию" to false,
        "құржию" to false,
        "қыдию" to false,
        "қылжию" to false,
        "қылмию" to false,
        "қылтию" to false,
        "қыржию" to false,
        "қыртию" to false,
        "лықию" to false,
        "маңқию" to false,
        "миқию" to false,
        "монтию" to false,
        "мықию" to false,
        "мықшию" to false,
        "мыржию" to false,
        "оқшию" to false,
        "сақию" to false,
        "сақсию" to false,
        "саңқию" to false,
        "сапсию" to false,
        "сидию" to false,
        "сойдию" to false,
        "соқию" to false,
        "солпию" to false,
        "сомпию" to false,
        "сопию" to false,
        "состию" to false,
        "сұстию" to false,
        "сықию" to false,
        "сықсию" to false,
        "сылқию" to false,
        "сымпию" to false,
        "сыптию" to false,
        "сырию" to false,
        "тайқию" to false,
        "тайпию" to false,
        "талпию" to false,
        "талтию" to false,
        "таңқию" to false,
        "тарбию" to false,
        "тарпию" to false,
        "тойтию" to false,
        "томпию" to false,
        "тоңқию" to false,
        "торсию" to false,
        "тортию" to false,
        "тостию" to false,
        "тотию" to false,
        "тұғжию" to false,
        "тұқию" to false,
        "тұқшию" to false,
        "тұштию" to false,
        "тылтию" to false,
        "тымпию" to false,
        "тыңқию" to false,
        "тырбию" to false,
        "тыржию" to false,
        "тырию" to false,
        "тырқию" to false,
        "тырсию" to false,
        "тыртию" to false,
        "шақшию" to false,
        "шалжию" to false,
        "шалқию" to false,
        "шанжию" to false,
        "шаңқию" to false,
        "шартию" to false,
        "шойқию" to false,
        "шоқию" to false,
        "шоқшию" to false,
        "шолжию" to false,
        "шолтию" to false,
        "шоңқию" to false,
        "шұқию" to false,
        "шұқшию" to false,
        "шұнтию" to false,
        "шықию" to false,
        "шылқию" to false,
        "ыздию" to false,
        "ыңқию" to false,
        "ыржию" to false,
        "ырсию" to false,

        // nouns
        "ми" to false
    )

    val VERB_PRESENT_TRANSITIVE_EXCEPTIONS1_SET: Set<String> = setOf(
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
        "іру"
    )

    val kDropLastVowelNouns = setOf(
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
    )

    val kOptionallyDropLastVowelNouns = setOf(
        "ауыз",
        "дауыс",
        "көрік",
        "қалып",
        "қарын",
        "қойын",
        "нарық",
        "тұрық",
    )

    val kBaseReplacementPKKh = mapOf<Char, Char>(
        'п' to 'б',
        'к' to 'г',
        'қ' to 'ғ'
    )
}