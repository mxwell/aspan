let assertFails = 0;

function T_ASSERT(condition: boolean, message: string) {
    if (!condition) {
        console.log("Condition violated: " + message);
        assertFails += 1;
    }
}

function T_EQ_ASSERT(expected, got, message) {
    T_ASSERT(expected == got, message + "Expected [" + expected + "], but got [" + got + "]");
}

let ALL_TESTS = [];

function testAll() {
    console.log("Starting testing...\n");
    assertFails = 0;
    let testsStarted = 0;
    let testsFailed = 0;
    let testsPassed = 0;
    for (const testDescr of ALL_TESTS) {
        testsStarted += 1;
        const failsBefore = assertFails;
        const name = testDescr[0];
        const test = testDescr[1];
        test();
        if (failsBefore != assertFails) {
            testsFailed += 1;
            console.log("\n------ TEST [" + name + "] FAILED! ------");
        } else {
            testsPassed += 1;
            console.log("------ TEST [" + name + "] PASSED! ------");
        }
    }
    console.log("\n=====");
    console.log(String(testsPassed) + " tests passed out of " + String(testsStarted) + " started tests");
    if (testsFailed > 0) {
        console.log(String(testsFailed) + " tests failed out of " + String(testsStarted) + " started tests");
    }
    console.log("=====\n");
}

/* Tests go below */

/** Template:

ALL_TESTS.push(["name", function() {

}]);

**/

ALL_TESTS.push(["basicStatementFormsTest", function() {
    let verbDictForm = "алу";
    let verbBuilder = new VerbBuilder(verbDictForm);
    const sentenceType = SentenceType.Statement;
    const expectedForms = [
        "аламын",
        "аламыз",
        "аласың",
        "аласыңдар",
        "аласыз",
        "аласыздар",
        "алады",
        "алады",
    ];
    let position = 0;
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            let s = verbBuilder.presentTransitiveForm(person, number, sentenceType);
            let expected = expectedForms[position];
            position += 1;
            T_EQ_ASSERT(expected, s, sentenceType + " forms of the verb " + verbDictForm + ": ");
        }
    }
}]);

ALL_TESTS.push(["basicNegativeFormsTest", function() {
    let verbDictForm = "келу";
    let verbBuilder = new VerbBuilder(verbDictForm);
    const sentenceType = SentenceType.Negative;
    const expectedForms = [
        "келмеймін",
        "келмейміз",
        "келмейсің",
        "келмейсіңдер",
        "келмейсіз",
        "келмейсіздер",
        "келмейді",
        "келмейді",
    ];
    let position = 0;
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            let s = verbBuilder.presentTransitiveForm(person, number, sentenceType);
            let expected = expectedForms[position];
            position += 1;
            T_EQ_ASSERT(expected, s, sentenceType + " forms of the verb " + verbDictForm + ": ");
        }
    }
}]);

ALL_TESTS.push(["basicQuestionFormsTest", function() {
    let verbDictForm = "ренжу";
    let verbBuilder = new VerbBuilder(verbDictForm);
    const sentenceType = SentenceType.Question;
    const expectedForms = [
        "ренжимін бе?",
        "ренжиміз бе?",
        "ренжисің бе?",
        "ренжисіңдер ме?",
        "ренжисіз бе?",
        "ренжисіздер ме?",
        "ренжи ме?",
        "ренжи ме?",
    ];
    let position = 0;
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            let s = verbBuilder.presentTransitiveForm(person, number, sentenceType);
            let expected = expectedForms[position];
            position += 1;
            T_EQ_ASSERT(expected, s, sentenceType + " forms of the verb " + verbDictForm + ": ");
        }
    }
}]);

ALL_TESTS.push(["trickyCasesTest", function() {
    T_EQ_ASSERT("ренжімеймін", new VerbBuilder("ренжу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
    T_EQ_ASSERT("ішпейміз", new VerbBuilder("ішу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Negative), "Tricky negative form: ");
    T_EQ_ASSERT("қиямын", new VerbBuilder("қию").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement), "Tricky form: ");
    T_EQ_ASSERT("киемін", new VerbBuilder("кию").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement), "Tricky form: ");
    T_EQ_ASSERT("кимеймін", new VerbBuilder("кию").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
    T_EQ_ASSERT("кие ме?", new VerbBuilder("кию").presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Question), "Tricky question form: ");
    T_EQ_ASSERT("оқимын", new VerbBuilder("оқу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement), "Tricky form: ");
    T_EQ_ASSERT("сүйемін", new VerbBuilder("сүю").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement), "Tricky form: ");
    T_EQ_ASSERT("ажуады", new VerbBuilder("ажуу").presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement), "Tricky form of an imaginary verb: ");

    // negative tricky
    T_EQ_ASSERT("қоймаймын", new VerbBuilder("қою").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
    T_EQ_ASSERT("естімеймін", new VerbBuilder("есту").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
    T_EQ_ASSERT("дамымайсыз", new VerbBuilder("даму").presentTransitiveForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
}]);

ALL_TESTS.push(["consonantChangeInNegativeFormsTest", function() {
    let dictFormToThirdPerson = [
        ["кешігу", "кешігеді", "кешікпейді"],
        ["тігу", "тігеді", "тікпейді"],
        ["шығу", "шығады", "шықпайды"],
        ["қызығу", "қызығады", "қызықпайды"],
        ["тебу", "тебеді", "теппейді"],
        ["жабу", "жабады", "жаппайды"],
    ];
    for (const [verbDictForm, statement, negative] of dictFormToThirdPerson) {
        let verbBuilder = new VerbBuilder(verbDictForm);
        let formStatement = verbBuilder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement);
        T_EQ_ASSERT(statement, formStatement, "3rd person form of verb ending with -гу/-ғу/-бу: ");
        let formNegative = verbBuilder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative);
        T_EQ_ASSERT(negative, formNegative, "3rd person negative form of verb ending with -гу/-ғу/-бу: ");
    }
}]);

ALL_TESTS.push(["verbExceptionsTest", function() {
    let dictFormToThirdPerson = new Map([
        ["абыржу", "абыржиды"],
        ["ағайынсу", "ағайынсиды"],
        ["адалсу", "адалсиды"],
        ["адамсу", "адамсиды"],
        ["айну", "айниды"],
        ["ақылгөйсу", "ақылгөйсиді"],
        ["ақылсу", "ақылсиды"],
        ["ақынсу", "ақынсиды"],
        ["алжу", "алжиды"],
        ["аңду", "аңдиды"],
        ["аңқаусу", "аңқаусиды"],
        ["аңқу", "аңқиды"],
        ["апшу", "апшиды"],
        ["арзу", "арзиды"],
        ["ару", "ариды"],
        ["аршу", "аршиды"],
        ["астамсу", "астамсиды"],
        ["атқу", "атқиды"],
        ["аунақшу", "аунақшиды"],
        ["ауытқу", "ауытқиды"],
        ["аярсу", "аярсиды"],
        ["аяқсу", "аяқсиды"],
        ["әзілдегенсу", "әзілдегенсиді"],
        ["әкімсу", "әкімсиді"],
        ["әсемсу", "әсемсиді"],
        ["әспенсу", "әспенсиді"],
        ["балқу", "балқиды"],
        ["балуансу", "балуансиды"],
        ["батпансу", "батпансиды"],
        ["батырсу", "батырсиды"],
        ["батырымсу", "батырымсиды"],
        ["баулу", "баулиды"],
        ["баяусу", "баяусиды"],
        ["бәлсу", "бәлсиді"],
        ["бәсеңсу", "бәсеңсиді"],
        ["бейкүнәмсу", "бейкүнәмсиді"],
        ["бейқамсу", "бейқамсиды"],
        ["беку", "бекиді"],
        ["берегенсу", "берегенсиді"],
        ["берку", "беркиді"],
        ["болғансу", "болғансиды"],
        ["боржу", "боржиды"],
        ["борсу", "борсиды"],
        ["босаңсу", "босаңсиды"],
        ["бөлексу", "бөлексиді"],
        ["бөтенсу", "бөтенсиді"],
        ["буазу", "буазиды"],
        ["бұлқу", "бұлқиды"],
        ["бұлықсу", "бұлықсиды"],
        ["быжу", "быжиды"],
        ["бықсу", "бықсиды"],
        ["бықу", "бықиды"],
        ["бықырсу", "бықырсиды"],
        ["былқу", "былқиды"],
        ["быршу", "быршиды"],
        ["білгенсу", "білгенсиді"],
        ["білгірсу", "білгірсиді"],
        ["білгішсу", "білгішсиді"],
        ["білдіргенсу", "білдіргенсиді"],
        ["даму", "дамиды"],
        ["данагөйсу", "данагөйсиді"],
        ["данасу", "данасиды"],
        ["дандайсу", "дандайсиды"],
        ["данышпансу", "данышпансиды"],
        ["даңғойсу", "даңғойсиды"],
        ["дардайсу", "дардайсиды"],
        ["дарқансу", "дарқансиды"],
        ["дару", "дариды"],
        ["дәнсу", "дәнсиді"],
        ["дәусу", "дәусиді"],
        ["дегду", "дегдиді"],
        ["дөңбекшу", "дөңбекшиді"],
        ["дөрекпу", "дөрекпиді"],
        ["дүмпу", "дүмпиді"],
        ["дүңку", "дүңкиді"],
        ["ділмарсу", "ділмарсиды"],
        ["діндарсу", "діндарсиды"],
        ["елегенсу", "елегенсиді"],
        ["елту", "елтиді"],
        ["емексу", "емексиді"],
        ["еркексу", "еркексиді"],
        ["еркесу", "еркесиді"],
        ["еркінсу", "еркінсиді"],
        ["ерсу", "ерсиді"],
        ["есту", "естиді"],
        ["есіркегенсу", "есіркегенсиді"],
        ["жағымсу", "жағымсиды"],
        ["жадыгөйсу", "жадыгөйсиді"],
        ["жайбарақатсу", "жайбарақатсиды"],
        ["жайдақсу", "жайдақсиды"],
        ["жақынсу", "жақынсиды"],
        ["жалғызсу", "жалғызсиды"],
        ["жалқаусу", "жалқаусиды"],
        ["жалқу", "жалқиды"],
        ["жану", "жаниды"],
        ["жаншу", "жаншиды"],
        ["жасу", "жасиды"],
        ["жаталақшу", "жаталақшиды"],
        ["жеку", "жекиді"],
        ["желпу", "желпиді"],
        ["жеңілгенсу", "жеңілгенсиді"],
        ["жеру", "жериді"],
        ["жиду", "жидиді"],
        ["жомартсу", "жомартсиды"],
        ["жору", "жориды"],
        ["жосу", "жосиды"],
        ["жөңку", "жөңкиді"],
        ["жуасу", "жуасиды"],
        ["жұлқу", "жұлқиды"],
        ["жүйтку", "жүйткиді"],
        ["жүнжу", "жүнжиді"],
        ["жыбыршу", "жыбыршиды"],
        ["жылжу", "жылжиды"],
        ["жылу", "жылиды"],
        ["жылымсу", "жылымсиды"],
        ["жылымшу", "жылымшиды"],
        ["жібу", "жібиді"],
        ["жігітсу", "жігітсиді"],
        ["жіпсу", "жіпсиді"],
        ["зеку", "зекиді"],
        ["зеңгу", "зеңгиді"],
        ["зырғу", "зырғиды"],
        ["кеберсу", "кеберсиді"],
        ["кебірсу", "кебірсиді"],
        ["кебіртексу", "кебіртексиді"],
        ["кедейсу", "кедейсиді"],
        ["кему", "кемиді"],
        ["кеңу", "кеңиді"],
        ["кепсу", "кепсиді"],
        ["кербезсу", "кербезсиді"],
        ["кергу", "кергиді"],
        ["кереметсу", "кереметсиді"],
        ["керсу", "керсиді"],
        ["көбеңсу", "көбеңсиді"],
        ["көгілжу", "көгілжиді"],
        ["көлгірсу", "көлгірсиді"],
        ["көлку", "көлкиді"],
        ["көнсу", "көнсиді"],
        ["көншу", "көншиді"],
        ["көңірсу", "көңірсиді"],
        ["көпсу", "көпсиді"],
        ["көпіршу", "көпіршиді"],
        ["көсемсу", "көсемсиді"],
        ["күлімсу", "күлімсиді"],
        ["күмілжу", "күмілжиді"],
        ["күнсу", "күнсиді"],
        ["күпсу", "күпсиді"],
        ["күпу", "күпиді"],
        ["кілку", "кілкиді"],
        ["кінәзсу", "кінәзсиді"],
        ["кісімсу", "кісімсиді"],
        ["қабаржу", "қабаржиды"],
        ["қағып-сілку", "қағып-сілкиді"],
        ["қағылжу", "қағылжиды"],
        ["қажу", "қажиды"],
        ["қаймақшу", "қаймақшиды"],
        ["қақсу", "қақсиды"],
        ["қақшу", "қақшиды"],
        ["қалғу", "қалғиды"],
        ["қалқу", "қалқиды"],
        ["қамқорсу", "қамқорсиды"],
        ["қамту", "қамтиды"],
        ["қаңғу", "қаңғиды"],
        ["қаңсу", "қаңсиды"],
        ["қарбу", "қарбиды"],
        ["қарғу", "қарғиды"],
        ["қарпу", "қарпиды"],
        ["қару", "қариды"],
        ["қасаңсу", "қасаңсиды"],
        ["қасу", "қасиды"],
        ["қобалжу", "қобалжиды"],
        ["қожайынсу", "қожайынсиды"],
        ["қоқсу", "қоқсиды"],
        ["қоқу", "қоқиды"],
        ["қоқырсу", "қоқырсиды"],
        ["қоңылтақсу", "қоңылтақсиды"],
        ["қору", "қориды"],
        ["құбылжу", "құбылжиды"],
        ["құдайсу", "құдайсиды"],
        ["құйқылжу", "құйқылжиды"],
        ["құлазу", "құлазиды"],
        ["құрғақсу", "құрғақсиды"],
        ["қылғу", "қылғиды"],
        ["қылпу", "қылпиды"],
        ["қылымсу", "қылымсиды"],
        ["қымқу", "қымқиды"],
        ["қымту", "қымтиды"],
        ["қыңсу", "қыңсиды"],
        ["қырпу", "қырпиды"],
        ["қыршу", "қыршиды"],
        ["қышу", "қышиды"],
        ["ләйлу", "ләйлиді"],
        ["леку", "лекиді"],
        ["лоблу", "лоблиды"],
        ["лоқсу", "лоқсиды"],
        ["лықсу", "лықсиды"],
        ["лықу", "лықиды"],
        ["лыпу", "лыпиды"],
        ["малту", "малтиды"],
        ["малшу", "малшиды"],
        ["манду", "мандиды"],
        ["маңғазсу", "маңғазсиды"],
        ["марғаусу", "марғаусиды"],
        ["мардамсу", "мардамсиды"],
        ["мәңгу", "мәңгиді"],
        ["менменсу", "менменсиді"],
        ["меңіреусу", "меңіреусиді"],
        ["мойынсу", "мойынсиды"],
        ["момақансу", "момақансиды"],
        ["мұжу", "мұжиды"],
        ["мүжу", "мүжиді"],
        ["мүләйімсу", "мүләйімсиді"],
        ["мүлгу", "мүлгиді"],
        ["мүңку", "мүңкиді"],
        ["мүсәпірсу", "мүсәпірсиді"],
        ["мығымсу", "мығымсиды"],
        ["мыжу", "мыжиды"],
        ["мызғу", "мызғиды"],
        ["мылқаусу", "мылқаусиды"],
        ["мырзасу", "мырзасиды"],
        ["мытқу", "мытқиды"],
        ["мыту", "мытиды"],
        ["міндетсу", "міндетсиді"],
        ["налу", "налиды"],
        ["нұқу", "нұқиды"],
        ["обалсу", "обалсиды"],
        ["ойнақшу", "ойнақшиды"],
        ["оқу", "оқиды"],
        ["орғу", "орғиды"],
        ["ортқу", "ортқиды"],
        ["оршу", "оршиды"],
        ["өгейсу", "өгейсиді"],
        ["өзімсу", "өзімсиді"],
        ["өксу", "өксиді"],
        ["өкімсу", "өкімсиді"],
        ["өрбу", "өрбиді"],
        ["өрекпу", "өрекпиді"],
        ["өрекшу", "өрекшиді"],
        ["өршу", "өршиді"],
        ["өсту", "өстиді"],
        ["өсіп-өрбу", "өсіп-өрбиді"],
        ["пақырсу", "пақырсиды"],
        ["палуансу", "палуансиды"],
        ["паңсу", "паңсиды"],
        ["пысықсу", "пысықсиды"],
        ["ренжу", "ренжиді"],
        ["салақсу", "салақсиды"],
        ["салғансу", "салғансиды"],
        ["салғыртсу", "салғыртсиды"],
        ["салқамсу", "салқамсиды"],
        ["салқынсу", "салқынсиды"],
        ["самарқаусу", "самарқаусиды"],
        ["самсу", "самсиды"],
        ["саңғу", "саңғиды"],
        ["сапсу", "сапсиды"],
        ["сараңсу", "сараңсиды"],
        ["сарқу", "сарқиды"],
        ["сарсу", "сарсиды"],
        ["сару", "сариды"],
        ["саябырсу", "саябырсиды"],
        ["саяқсу", "саяқсиды"],
        ["сәнсу", "сәнсиді"],
        ["сәуегейсу", "сәуегейсиді"],
        ["сенгенсу", "сенгенсиді"],
        ["сепсу", "сепсиді"],
        ["сергексу", "сергексиді"],
        ["сергу", "сергиді"],
        ["серпу", "серпиді"],
        ["серімсу", "серімсиді"],
        ["сету", "сетиді"],
        ["сирексу", "сирексиді"],
        ["сорғу", "сорғиды"],
        ["сусу", "сусиды"],
        ["суу", "суиды"],
        ["сүңгу", "сүңгиді"],
        ["сылту", "сылтиды"],
        ["сылу", "сылиды"],
        ["сыңсу", "сыңсиды"],
        ["сыпайысу", "сыпайысиды"],
        ["сырғақсу", "сырғақсиды"],
        ["сырғу", "сырғиды"],
        ["сыру", "сыриды"],
        ["сілку", "сілкиді"],
        ["тайқақсу", "тайқақсиды"],
        ["тайқу", "тайқиды"],
        ["талмаусу", "талмаусиды"],
        ["талықсу", "талықсиды"],
        ["тамылжу", "тамылжиды"],
        ["танту", "тантиды"],
        ["тарпу", "тарпиды"],
        ["тартқансу", "тартқансиды"],
        ["тасу", "тасиды"],
        ["тәкаппарсу", "тәкаппарсиды"],
        ["тәлімсу", "тәлімсиді"],
        ["тәңірсу", "тәңірсиді"],
        ["тәуелжу", "тәуелжиді"],
        ["тәуірсу", "тәуірсиді"],
        ["телу", "телиді"],
        ["тепшу", "тепшиді"],
        ["терлеп-тепшу", "терлеп-тепшиді"],
        ["тершу", "тершиді"],
        ["тетку", "теткиді"],
        ["тобарсу", "тобарсиді"],
        ["тоқмейілсу", "тоқмейілсиді"],
        ["тоқу", "тоқиды"],
        ["толқу", "толқиды"],
        ["толықсу", "толықсиды"],
        ["тоңазу", "тоңазиды"],
        ["тору", "ториды"],
        ["төменсу", "төменсиді"],
        ["тұшу", "тұшиды"],
        ["түйткілжу", "түйткілжиді"],
        ["түйткілсу", "түйткілсиді"],
        ["түлежу", "түлежиді"],
        ["тықыршу", "тықыршиды"],
        ["тыншу", "тыншиды"],
        ["тыпыршу", "тыпыршиды"],
        ["тілмарсу", "тілмарсиды"],
        ["уылжу", "уылжиды"],
        ["ұйтқу", "ұйтқиды"],
        ["ұлу", "ұлиды"],
        ["ұлықсу", "ұлықсиды"],
        ["ұңғу", "ұңғиды"],
        ["үлкенсу", "үлкенсиді"],
        ["үңгу", "үңгиді"],
        ["үстемсу", "үстемсиді"],
        ["үсу", "үсиді"],
        ["шалқу", "шалқиды"],
        ["шанду", "шандиды"],
        ["шаншу", "шаншиды"],
        ["шапшу", "шапшиды"],
        ["шарпу", "шарпиды"],
        ["шеку", "шекиді"],
        ["шешенсу", "шешенсиді"],
        ["шоқу", "шоқиды"],
        ["шоршу", "шоршиды"],
        ["шошу", "шошиды"],
        ["шөжу", "шөжиді"],
        ["шөку", "шөкиді"],
        ["шөпшу", "шөпшиді"],
        ["шұқу", "шұқиды"],
        ["шұлғу", "шұлғиды"],
        ["шүйгу", "шүйгиді"],
        ["шүленсу", "шүленсиді"],
        ["шыжу", "шыжиды"],
        ["шылқу", "шылқиды"],
        ["шымшу", "шымшиды"],
        ["шыпшу", "шыпшиды"],
        ["шырпу", "шырпиды"],
        ["шіру", "шіриді"],
        ["ыбылжу", "ыбылжиды"],
        ["ыбырсу", "ыбырсиды"],
        ["ызғу", "ызғиды"],
        ["ыңырсу", "ыңырсиды"],
        ["ырғу", "ырғиды"],
        ["ыршу", "ыршиды"],
        ["ытқу", "ытқиды"],
        ["ілбу", "ілбиді"],
        ["іру", "іриді"],
    ]);
    for (const [verbDictForm, thirdPerson] of Array.from(dictFormToThirdPerson.entries())) {
        const verbBuilder = new VerbBuilder(verbDictForm);
        const form = verbBuilder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement);
        T_EQ_ASSERT(thirdPerson, form, "Exception verb 3rd person form: ");
    }
}]);

ALL_TESTS.push(["verbOptionalExceptionsTest", function() {
    let dictFormToThirdPerson = [
        ["ашу", "ашады", "ашиды"],
        ["еру", "ереді", "ериді"],
        ["жару", "жарады", "жариды"],
        ["жуу", "жуады", "жуиды"],
        ["қабу", "қабады", "қабиды"],
        ["құру", "құрады", "құриды"],
        ["пысу", "пысады", "пысиды"],
        ["сасу", "сасады", "сасиды"],
        ["тану", "танады", "таниды"],
        ["тату", "татады", "татиды"],
        ["ысу", "ысады", "ысиды"],
    ];
    for (const [verbDictForm, thirdPersonRegular, thirdPersonException] of dictFormToThirdPerson) {
        const verbBuilderRegular = new VerbBuilder(verbDictForm);
        const formRegular = verbBuilderRegular.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement);
        T_EQ_ASSERT(thirdPersonRegular, formRegular, "Optional exception verb, 3rd person regular form: ");
        const verbBuilderException = new VerbBuilder(verbDictForm, true);
        const formException = verbBuilderException.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement);
        T_EQ_ASSERT(thirdPersonException, formException, "Optional exception verb, 3rd person exception form: ");
    }
}]);

ALL_TESTS.push(["verbExceptionsWithEndingYu", function() {
    let dictFormToThirdPerson = [
        // hard sound cases
        ["азаю", "азай", "азаяды"],
        ["жаю", "жай", "жаяды"],
        ["зораю", "зорай", "зораяды"],
        ["қартаю", "қартай", "қартаяды"],
        ["құю", "құй", "құяды"],
        ["қыжыраю", "қыжырай", "қыжыраяды"],
        ["масаю", "масай", "масаяды"],
        ["тою", "той", "тояды"],
        // soft sound cases
        ["бәсею", "бәсей", "бәсейеді"],
        ["еңкею", "еңкей", "еңкейеді"],
        ["ересею", "ересей", "ересейеді"],
        ["көбею", "көбей", "көбейеді"],
        ["көркею", "көркей", "көркейеді"],
        ["күю", "күй", "күйеді"],
        ["серею", "серей", "серейеді"],
        ["сүю", "сүй", "сүйеді"],
        // exceptions
        ["баю", "байы", "байиды"],
        ["кею", "кейі", "кейиді"],
        ["қаю", "қайы", "қайиды"],
        ["мою", "мойы", "мойиды"],
        ["ұю", "ұйы", "ұйиды"],
        // ending with -ию, hard sound cases
        ["аңқию", "аңқи", "аңқияды"],
        ["қию", "қи", "қияды"],
        ["балпию", "балпи", "балпияды"],
        ["дардию", "дарди", "дардияды"],
        ["жарбию", "жарби", "жарбияды"],
        ["қалтию", "қалти", "қалтияды"],
        // ending with -ию, soft sound cases
        ["дүмпию", "дүмпи", "дүмпиеді"],
        ["ербию", "ерби", "ербиеді"],
        ["итию", "ити", "итиеді"],
        ["кіржию", "кіржи", "кіржиеді"],
        ["сербию", "серби", "сербиеді"],
    ];
    for (const [verbDictForm, verbBase, thirdPersonStatement] of dictFormToThirdPerson) {
        const verbBuilder = new VerbBuilder(verbDictForm);
        const formNegative = verbBuilder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative);
        T_ASSERT(
            formNegative.startsWith(verbBase),
            "Negative form of exception verb " + verbDictForm + " must start with " + verbBase + " but got " + formNegative
        );
        const formStatement = verbBuilder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement);
        T_EQ_ASSERT(thirdPersonStatement, formStatement, "Exception verb (ending with -ю), 3rd person form: ");
    }
}]);

ALL_TESTS.push(["testVerbSounds", function() {
    let verbBuilder = new VerbBuilder("тігу");
    let s = verbBuilder.presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative);
    T_EQ_ASSERT("тікпеймін", s, "Bad sounding consonant bigrams must be fixed: ");
}]);

ALL_TESTS.push(["softTest", function() {
    const softWords = [
        "дәус",
        "зек",
    ];
    for (const word of softWords) {
        T_ASSERT(wordIsSoft(word), "Word " + word + " must be soft");
    }
}]);

/* End of tests */

testAll();