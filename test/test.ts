let assertFails = 0;
let assertOks = 0;

function T_ASSERT(condition: boolean, message: string) {
    if (!condition) {
        console.log("Condition violated: " + message);
        assertFails += 1;
    } else {
        assertOks += 1;
    }
}

function T_EQ_STR_ASSERT(expected: string, got: string, message: string) {
    T_ASSERT(expected == got, message + ": Expected [" + expected + "], but got [" + got + "]");
}

function T_EQ_ASSERT(expected: string, got: Phrasal, message: string) {
    T_EQ_STR_ASSERT(expected, got.raw, message);
}

let ALL_TESTS: [string, () => void][] = [];

function testAll() {
    console.log("Starting testing...\n");
    assertFails = 0;
    let testsStarted = 0;
    let testsFailed = 0;
    let testsPassed = 0;
    let testsBad = 0;
    let usedNames = new Set();
    for (const testDescr of ALL_TESTS) {
        testsStarted += 1;
        const failsBefore = assertFails;
        const oksBefore = assertOks;
        const name = testDescr[0];
        if (usedNames.has(name)) {
            console.log("Test " + name + " has duplicate name!");
            testsBad += 1;
        } else {
            usedNames.add(name);
        }
        const test = testDescr[1];
        test();
        const gainedFails = assertFails - failsBefore;
        const gainedOks = assertOks - oksBefore;
        const denominator = gainedFails + gainedOks;
        const testFailed = failsBefore != assertFails;
        const status = testFailed ? "FAILED" : "PASSED";
        if (testFailed) {
            testsFailed += 1;
            console.log("\n");
        } else {
            testsPassed += 1;
        }
        console.log(`------ TEST [${name} - ${gainedOks}/${denominator}] ${status}! ------`);
    }
    console.log("\n=====");
    console.log(String(testsPassed) + " tests passed out of " + String(testsStarted) + " started tests");
    if (testsFailed > 0) {
        console.log(String(testsFailed) + " tests failed out of " + String(testsStarted) + " started tests");
    }
    if (testsBad > 0) {
        console.log(String(testsBad) + " tests are malformed!");
    }
    console.log("=====\n");
}

class VerbSpec {
    verbDictForm: string;
    forceExceptional: boolean;
    constructor(verbDictForm: string, forceExceptional: boolean) {
        this.verbDictForm = verbDictForm
        this.forceExceptional = forceExceptional
    }
}

type VerbFormProducer = (verbBuilder: VerbBuilder, grammarPerson: GrammarPerson, grammarNumber: GrammarNumber, sentenceType: SentenceType) => Phrasal;

function testAllCases(testName: string, verbDictForm: string, sentenceType: SentenceType, callback: VerbFormProducer, expectedForms: string[]) {
    let verbBuilder = new VerbBuilder(verbDictForm);
    let position = 0;
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            let result = callback(verbBuilder, person, number, sentenceType);
            let expected = expectedForms[position];
            position += 1;
            T_EQ_ASSERT(expected, result, `Test ${testName}, ${person}, ${number}, ${sentenceType}: `);
        }
    }
}

type MaybePartExplanationType = PART_EXPLANATION_TYPE | null;

function checkPartExplanations(testName: String, phrasal: Phrasal, explanationTypes: MaybePartExplanationType[], soft: boolean) {
    for (let i = 0; i < explanationTypes.length; i++) {
        let explanationType = explanationTypes[i];
        let part = phrasal.parts[i];
        let explanation = part.explanation;
        if (explanationType == null) {
            T_ASSERT(explanation == null, `Test ${testName}, part ${i}, explanation null`);
        } else {
            T_EQ_STR_ASSERT(explanationType, explanation.explanationType, `Test ${testName}, part ${i}, explanation type`);
            T_ASSERT(explanation.soft == soft, `Test ${testName}, part ${i}, explanation soft`);
        }
    }
}

/* Tests go below */

/** Template:

ALL_TESTS.push(["name", function() {

}]);

**/

ALL_TESTS.push(["basicStatementFormsTest", function() {
    testAllCases(
        "basicStatementAllForms",
        "алу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.presentTransitiveForm(grammarPerson, grammarNumber, sentenceType);
        },
        [ "аламын", "аламыз", "аласың", "аласыңдар", "аласыз", "аласыздар", "алады", "алады" ],
    );
}]);

ALL_TESTS.push(["presentTransitiveExplanationTest", function() {
    checkPartExplanations(
        "presentTransitiveExplanationTest",
        new VerbBuilder("кесу").presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        [
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        true,
    );

    checkPartExplanations(
        "presentTransitiveExplanationTest",
        new VerbBuilder("жасау").presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        [
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        false,
    );
}]);

ALL_TESTS.push(["presentTransitiveTrickyExplanationTest", function() {
    checkPartExplanations(
        "presentTransitiveTrickyExplanationTest",
        new VerbBuilder("ренжу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        [
            PART_EXPLANATION_TYPE.VerbBaseLostY,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitiveToYi,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        true,
    );

    checkPartExplanations(
        "presentTransitiveTrickyExplanationTest",
        new VerbBuilder("оқу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        [
            PART_EXPLANATION_TYPE.VerbBaseLostY,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitiveToYi,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        false,
    );

    checkPartExplanations(
        "presentTransitiveTrickyExplanationTest",
        new VerbBuilder("баю").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        [
            PART_EXPLANATION_TYPE.VerbBaseGainIShortLoseY,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitiveToYi,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        false,
    );
}]);

ALL_TESTS.push(["presTransNegExplTest", function() {
    checkPartExplanations(
        "presTransNegExplTest",
        new VerbBuilder("кесу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        [
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            PART_EXPLANATION_TYPE.VerbNegationPostBase,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        true,
    );
    checkPartExplanations(
        "presTransNegExplTest",
        new VerbBuilder("қорқу").presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        [
            PART_EXPLANATION_TYPE.VerbBaseGainedYInsidePriorCons,
            PART_EXPLANATION_TYPE.VerbNegationPostBase,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
        ],
        false,
    );
}]);

ALL_TESTS.push(["presTransQuestionExplTest", function() {
    let builder = new VerbBuilder("қуану");

    checkPartExplanations(
        "presTransFirstPersQuestionExplTest",
        builder.presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        [
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitive,
            null,
            PART_EXPLANATION_TYPE.QuestionParticleSeparate,
        ],
        false,
    );

    checkPartExplanations(
        "presTransThirdPersQuestionExplTest",
        builder.presentTransitiveForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        [
            PART_EXPLANATION_TYPE.VerbBaseStripU,
            PART_EXPLANATION_TYPE.VerbTenseAffixPresentTransitive,
            PART_EXPLANATION_TYPE.VerbPersonalAffixPresentTransitiveQuestionSkip,
            null,
            PART_EXPLANATION_TYPE.QuestionParticleSeparate,
        ],
        false,
    );
}]);

ALL_TESTS.push(["basicNegativeFormsTest", function() {
    testAllCases(
        "basicNegativeAllForms",
        "келу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.presentTransitiveForm(grammarPerson, grammarNumber, sentenceType);
        },
        [ "келмеймін", "келмейміз", "келмейсің", "келмейсіңдер", "келмейсіз", "келмейсіздер", "келмейді", "келмейді" ],
    );
    testAllCases(
        "Present transitive negative, special verb, all cases",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.presentTransitiveForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпаймын", "қорықпаймыз", "қорықпайсың", "қорықпайсыңдар", "қорықпайсыз", "қорықпайсыздар", "қорықпайды", "қорықпайды"],
    );
}]);

ALL_TESTS.push(["basicQuestionFormsTest", function() {
    testAllCases(
        "basicQuestionAllForms",
        "ренжу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.presentTransitiveForm(grammarPerson, grammarNumber, sentenceType);
        },
        [ "ренжимін бе?", "ренжиміз бе?", "ренжисің бе?", "ренжисіңдер ме?", "ренжисіз бе?", "ренжисіздер ме?", "ренжи ме?", "ренжи ме?" ],
    );
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
            formNegative.raw.startsWith(verbBase),
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

ALL_TESTS.push(["simplePresentContTest", function() {
    const table: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
        First: {
            Singular: ["тұрмын", "жүрмін", "отырмын", "жатырмын"],
            Plural: ["тұрмыз", "жүрміз", "отырмыз", "жатырмыз"],
        },
        Second: {
            Singular: ["тұрсың", "жүрсің", "отырсың", "жатырсың"],
            Plural: ["тұрсыңдар", "жүрсіңдер", "отырсыңдар", "жатырсыңдар"],
        },
        SecondPolite: {
            Singular: ["тұрсыз", "жүрсіз", "отырсыз", "жатырсыз"],
            Plural: ["тұрсыздар", "жүрсіздер", "отырсыздар", "жатырсыздар"],
        },
        Third: {
            Singular: ["тұр", "жүр", "отыр", "жатыр"],
            Plural: ["тұр", "жүр", "отыр", "жатыр"],
        },
    };
    let verbBuilders = [];
    let verbDictForms = ["тұру", "жүру", "отыру", "жату"];
    for (const verb of verbDictForms) {
        verbBuilders.push(new VerbBuilder(verb));
    }
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            const expectedForms: string[] = table[person][number];
            for (var i = 0; i < expectedForms.length; ++i) {
                const form = verbBuilders[i].presentSimpleContinuousForm(person, number, SentenceType.Statement);
                T_EQ_ASSERT(expectedForms[i], form, "Simple present tense of " + person + " person, " + number + ": ");
            }
        }
    }
    T_EQ_ASSERT(
        "<not supported>",
        new VerbBuilder("алу").presentSimpleContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Only 4 verbs can have present simple continuous form: "
    );
}]);

ALL_TESTS.push(["negativePresentContTest", function() {
    const table: Record<GrammarPerson, Record<GrammarNumber, string[]>> = {
        First: {
            Singular: ["тұрған жоқпын", "жүрген жоқпын", "отырған жоқпын", "жатқан жоқпын"],
            Plural: ["тұрған жоқпыз", "жүрген жоқпыз", "отырған жоқпыз", "жатқан жоқпыз"],
        },
        Second: {
            Singular: ["тұрған жоқсың", "жүрген жоқсың", "отырған жоқсың", "жатқан жоқсың"],
            Plural: ["тұрған жоқсыңдар", "жүрген жоқсыңдар", "отырған жоқсыңдар", "жатқан жоқсыңдар"],
        },
        SecondPolite: {
            Singular: ["тұрған жоқсыз", "жүрген жоқсыз", "отырған жоқсыз", "жатқан жоқсыз"],
            Plural: ["тұрған жоқсыздар", "жүрген жоқсыздар", "отырған жоқсыздар", "жатқан жоқсыздар"],
        },
        Third: {
            Singular: ["тұрған жоқ", "жүрген жоқ", "отырған жоқ", "жатқан жоқ"],
            Plural: ["тұрған жоқ", "жүрген жоқ", "отырған жоқ", "жатқан жоқ"],
        },
    };
    let verbBuilders = [];
    let verbDictForms = ["тұру", "жүру", "отыру", "жату"];
    for (const verb of verbDictForms) {
        verbBuilders.push(new VerbBuilder(verb));
    }
    for (const person of GRAMMAR_PERSONS) {
        for (const number of GRAMMAR_NUMBERS) {
            const expectedForms = table[person][number];
            for (var i = 0; i < expectedForms.length; ++i) {
                const form = verbBuilders[i].presentSimpleContinuousForm(person, number, SentenceType.Negative);
                T_EQ_ASSERT(expectedForms[i], form, "Simple present tense of " + person + " person, " + number + ", negative: ");
            }
        }
    }
}]);

ALL_TESTS.push(["presentContBasicTest", function() {
    const verbBuilder = new VerbBuilder("сөйлеу");
    const auxVerbToForm = [
        ["тұру", "сөйлеп тұрмын"],
        ["жүру", "сөйлеп жүрмін"],
        ["отыру", "сөйлеп отырмын"],
        ["жату", "сөйлеп жатырмын"],
    ];

    for (const [auxVerb, expectedForm] of auxVerbToForm) {
        const auxVerbBuilder = new VerbBuilder(auxVerb);
        const form = verbBuilder.presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxVerbBuilder);
        T_EQ_ASSERT(expectedForm, form, "Present continuous form: ");
    }
}]);

ALL_TESTS.push(["presentContTest", function() {
    T_EQ_ASSERT(
        "жуып тұр",
        new VerbBuilder("жуу").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement, new VerbBuilder("тұру")),
        "Present continuous form of 3rd person, plural: "
    );
    T_EQ_ASSERT(
        "қарап тұрсың",
        new VerbBuilder("қарау").presentContinuousForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("тұру")),
        "Present continuous form of 2nd person, singular: "
    );
    T_EQ_ASSERT(
        "қыдырып жүрмін",
        new VerbBuilder("қыдыру").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("жүру")),
        "Present continuous form of 1st person, singular: "
    );
    T_EQ_ASSERT(
        "оқып жүрсіз",
        new VerbBuilder("оқу").presentContinuousForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("жүру")),
        "Present continuous form of 2nd polite person, singular: "
    );
    T_EQ_ASSERT(
        "алып жатыр",
        new VerbBuilder("алу").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("жату")),
        "Present continuous form of 3rd person, singular: "
    );
    T_EQ_ASSERT(
        "бара жатыр",
        new VerbBuilder("бару").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("жату")),
        "Present continuous form of 3rd person, singular: "
    );
    T_EQ_ASSERT(
        "келе жатырмын",
        new VerbBuilder("келу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, new VerbBuilder("жату")),
        "Present continuous form of 1st person, singular: "
    );
    T_EQ_ASSERT(
        "теуіп жүрміз",
        new VerbBuilder("тебу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Statement, new VerbBuilder("жүру")),
        "Present continuous form of 1st person, plural: "
    );
    T_EQ_ASSERT(
        "шауып жүрсіңдер",
        new VerbBuilder("шабу").presentContinuousForm(GrammarPerson.Second, GrammarNumber.Plural, SentenceType.Statement, new VerbBuilder("жүру")),
        "Present continuous form of 2nd person, plural: "
    );
    T_EQ_ASSERT(
        "қойып жатыр",
        new VerbBuilder("қою").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement, new VerbBuilder("жату")),
        "Present continuous form of 3rd person, plural: "
    );
    T_EQ_ASSERT(
        "сүйіп жатыр",
        new VerbBuilder("сүю").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement, new VerbBuilder("жату")),
        "Present continuous form of 3rd person, plural: "
    );
}]);

ALL_TESTS.push(["presentContReplaceUTest", function() {
    const relations = new Map([
        [new VerbSpec("жабу", false), "жауып жатырмын"],
        [new VerbSpec("қабу", false), "қауып жатырмын"],
        [new VerbSpec("қабу", true), "қабып жатырмын"],
        [new VerbSpec("табу", false), "тауып жатырмын"],
        [new VerbSpec("шабу", false), "шауып жатырмын"],
        [new VerbSpec("кебу", false), "кеуіп жатырмын"],
        [new VerbSpec("себу", false), "сеуіп жатырмын"],
        [new VerbSpec("тебу", false), "теуіп жатырмын"],
        [new VerbSpec("өбу", false), "өбіп жатырмын"],
    ]);
    const auxBuilder = new VerbBuilder("жату");
    for (const [spec, form] of Array.from(relations.entries())) {
        T_EQ_ASSERT(
            form,
            new VerbBuilder(spec.verbDictForm, spec.forceExceptional).presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, auxBuilder),
            "Present continuous, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["presentContNegativeReplaceUTest", function() {
    const relations = new Map([
        [new VerbSpec("жабу", false), "жауып жатқан жоқпын"],
        [new VerbSpec("қабу", false), "қауып жатқан жоқпын"],
        [new VerbSpec("қабу", true), "қабып жатқан жоқпын"],
        [new VerbSpec("табу", false), "тауып жатқан жоқпын"],
        [new VerbSpec("шабу", false), "шауып жатқан жоқпын"],
        [new VerbSpec("кебу", false), "кеуіп жатқан жоқпын"],
        [new VerbSpec("себу", false), "сеуіп жатқан жоқпын"],
        [new VerbSpec("тебу", false), "теуіп жатқан жоқпын"],
        [new VerbSpec("өбу", false), "өбіп жатқан жоқпын"],
    ]);
    const auxBuilder = new VerbBuilder("жату");
    for (const [spec, form] of Array.from(relations.entries())) {
        T_EQ_ASSERT(
            form,
            new VerbBuilder(spec.verbDictForm, spec.forceExceptional).presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative, auxBuilder),
            "Present continuous, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["presentContQuestionReplaceUTest", function() {
    const relations = [
        ["жабу", "жауып жатырмын ба?"],
        ["қабу", "қауып жатырмын ба?"],
        ["табу", "тауып жатырмын ба?"],
        ["шабу", "шауып жатырмын ба?"],
        ["кебу", "кеуіп жатырмын ба?"],
        ["себу", "сеуіп жатырмын ба?"],
        ["тебу", "теуіп жатырмын ба?"],
        ["өбу", "өбіп жатырмын ба?"],
    ];
    const auxBuilder = new VerbBuilder("жату");
    for (let i in relations) {
        let base = relations[i][0];
        let form = relations[i][1];
        T_EQ_ASSERT(
            form,
            new VerbBuilder(base).presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question, auxBuilder),
            "Present continuous, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["presentContNegativeTest", function() {
    T_EQ_ASSERT(
        "жазып отырған жоқсың",
        new VerbBuilder("жазу").presentContinuousForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Negative, new VerbBuilder("отыру")),
        "Present continuous form of 2nd person, singular, negative: "
    );
    T_EQ_ASSERT(
        "ішіп отырған жоқ",
        new VerbBuilder("ішу").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative, new VerbBuilder("отыру")),
        "Present continuous form of 3rd person, singular, negative: "
    );
    T_EQ_ASSERT(
        "оқып жүрген жоқпын",
        new VerbBuilder("оқу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative, new VerbBuilder("жүру")),
        "Present continuous form of 1st person, singular, negative: "
    );
}]);

ALL_TESTS.push(["presentContQuestionTest", function() {
    T_EQ_ASSERT(
        "жазып отырмын ба?",
        new VerbBuilder("жазу").presentContinuousForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question, new VerbBuilder("отыру")),
        "Present continuous form of 1st person, singular, question"
    );
    T_EQ_ASSERT(
        "бара жатыр ма?",
        new VerbBuilder("бару").presentContinuousForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question, new VerbBuilder("жату")),
        "Present continuous form of 3rd person, singular, question"
    );
}]);

ALL_TESTS.push(["presentContSimpleNegativeTest", function() {
    T_EQ_ASSERT(
        "жазбай отырсың",
        new VerbBuilder("жазу").presentContinuousSimpleNegativeForm(GrammarPerson.Second, GrammarNumber.Singular, new VerbBuilder("отыру")),
        "Present continuous form of 2nd person, singular, simple negative"
    );
    T_EQ_ASSERT(
        "ұйықтамай жатырмыз",
        new VerbBuilder("ұйықтау").presentContinuousSimpleNegativeForm(GrammarPerson.First, GrammarNumber.Plural, new VerbBuilder("жату")),
        "Present continuous form of 1st person, plural, simple negative"
    );

    T_EQ_ASSERT(
        "алмай жүрмін",
        new VerbBuilder("алу").presentContinuousSimpleNegativeForm(GrammarPerson.First, GrammarNumber.Singular, new VerbBuilder("жүру")),
        "Present continuous form of 1st person, singular, simple negative"
    );
}]);

ALL_TESTS.push(["wantClauseTest", function() {
    T_EQ_ASSERT(
        "отырғым келеді",
        new VerbBuilder("отыру").wantClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, VerbShak.PresentTransitive),
        "Want-clause of 1st person, singular, statement in present transitive"
    );
    T_EQ_ASSERT(
        "көргіміз келіп жатыр",
        new VerbBuilder("көру").wantClause(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Statement, VerbShak.PresentContinuous),
        "Want-clause of 1st person, plural, statement in present continuous"
    );
    T_EQ_ASSERT(
        "айтқың келіп жатыр ма?",
        new VerbBuilder("айту").wantClause(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question, VerbShak.PresentContinuous),
        "Want-clause of 2nd person, singular, question in present continuous"
    );
    T_EQ_ASSERT(
        "айналысқың келе ме?",
        new VerbBuilder("айналысу").wantClause(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question, VerbShak.PresentTransitive),
        "Want-clause of 2nd person, singular, question in present transitive"
    );

    T_EQ_ASSERT(
        "жапқыларың келеді",
        new VerbBuilder("жабу").wantClause(GrammarPerson.Second, GrammarNumber.Plural, SentenceType.Statement, VerbShak.PresentTransitive),
        "Want-clause of 2nd person, plural, statement in present transitive"
    );
    T_EQ_ASSERT(
        "оқығыңыз келе ме?",
        new VerbBuilder("оқу").wantClause(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question, VerbShak.PresentTransitive),
        "Want-clause of 2nd person polite, singular, question in present transitive"
    );
    T_EQ_ASSERT(
        "тепкіміз келеді",
        new VerbBuilder("тебу").wantClause(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Statement, VerbShak.PresentTransitive),
        "Want-clause of 1st person, plural, statement in present transitive"
    );

    T_EQ_ASSERT(
        "жазғыларыңыз келмейді",
        new VerbBuilder("жазу").wantClause(GrammarPerson.SecondPolite, GrammarNumber.Plural, SentenceType.Negative, VerbShak.PresentTransitive),
        "Want-clause of 2nd person polite, plural, negative in present transitive"
    );

    T_EQ_ASSERT(
        "ішкісі келмей жатыр",
        new VerbBuilder("ішу").wantClause(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative, VerbShak.PresentContinuous),
        "Want-clause of 3rd person, singular, negative in present continuous"
    );
}]);

ALL_TESTS.push(["wantClauseAllCasesTest", function() {
    testAllCases(
        "Want clause special case",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.wantClause(grammarPerson, grammarNumber, sentenceType, VerbShak.PresentTransitive);
        },
        ["қорыққым келеді", "қорыққымыз келеді", "қорыққың келеді", "қорыққыларың келеді", "қорыққыңыз келеді", "қорыққыларыңыз келеді", "қорыққысы келеді", "қорыққылары келеді"]
    );
    testAllCases(
        "Want clause special case",
        "ірку",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.wantClause(grammarPerson, grammarNumber, sentenceType, VerbShak.PresentTransitive);
        },
        ["іріккім келеді", "іріккіміз келеді", "іріккің келеді", "іріккілерің келеді", "іріккіңіз келеді", "іріккілеріңіз келеді", "іріккісі келеді", "іріккілері келеді"]
    );

}]);

ALL_TESTS.push(["canClauseTest", function() {
    T_EQ_ASSERT(
        "аулай аламын",
        new VerbBuilder("аулау").canClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, VerbShak.PresentTransitive),
        "Can-clause of 1st person, singular in present transitive"
    );
    T_EQ_ASSERT(
        "жаза алмаймын",
        new VerbBuilder("жазу").canClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative, VerbShak.PresentTransitive),
        "Can-clause of 1st person, singular, negative in present transitive"
    );
    T_EQ_ASSERT(
        "пісіре ала ма?",
        new VerbBuilder("пісіру").canClause(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question, VerbShak.PresentTransitive),
        "Can-clause of 3rd person, singular, question in present transitive"
    );
    T_EQ_ASSERT(
        "оқи алады",
        new VerbBuilder("оқу").canClause(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement, VerbShak.PresentTransitive),
        "Can-clause of 3rd person, singular, statement in present transitive"
    );
    T_EQ_ASSERT(
        "тоқи аламын",
        new VerbBuilder("тоқу").canClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, VerbShak.PresentTransitive),
        "Can-clause of 1st person, singular, statement in present transitive"
    );
    T_EQ_ASSERT(
        "қоя аламын",
        new VerbBuilder("қою").canClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement, VerbShak.PresentTransitive),
        "Can-clause of 1st person, singular, statement in present transitive"
    );

    T_EQ_ASSERT(
        "көмектесе алмай жатырмын",
        new VerbBuilder("көмектесу").canClause(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative, VerbShak.PresentContinuous),
        "Can-clause of 1st person, singular, negative in present continuous"
    );
    T_EQ_ASSERT(
        "үлгере алмай жатыр",
        new VerbBuilder("үлгеру").canClause(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative, VerbShak.PresentContinuous),
        "Can-clause of 3rd person, singular, negative in present continuous"
    );
}]);

ALL_TESTS.push(["pastTenseTest", function() {
    T_EQ_ASSERT(
        "жаздым",
        new VerbBuilder("жазу").pastForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Past tense form of 1st person, singular, statement"
    );
    T_EQ_ASSERT(
        "биледік",
        new VerbBuilder("билеу").pastForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Statement),
        "Past tense form of 1st person, plural, statement"
    );
    T_EQ_ASSERT(
        "отырдың",
        new VerbBuilder("отыру").pastForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Statement),
        "Past tense form of 2nd person, singular, statement"
    );

    T_EQ_ASSERT(
        "пісірді ме?",
        new VerbBuilder("пісіру").pastForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        "Past tense form of 3rd person, singular, question"
    );
    T_EQ_ASSERT(
        "болдыңдар ма?",
        new VerbBuilder("болу").pastForm(GrammarPerson.Second, GrammarNumber.Plural, SentenceType.Question),
        "Past tense form of 2nd person, plural, question"
    );
    T_EQ_ASSERT(
        "істедіңіздер ме?",
        new VerbBuilder("істеу").pastForm(GrammarPerson.SecondPolite, GrammarNumber.Plural, SentenceType.Question),
        "Past tense form of 2nd person polite, plural, question"
    );
    T_EQ_ASSERT(
        "оқыдыңыз ба?",
        new VerbBuilder("оқу").pastForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question),
        "Past tense form of 2nd person polite, singular, question"
    );

    T_EQ_ASSERT(
        "тыңдамады",
        new VerbBuilder("тыңдау").pastForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative),
        "Past tense form of 3rd person, singular, negative"
    );
    T_EQ_ASSERT(
        "ішпедік",
        new VerbBuilder("ішу").pastForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Negative),
        "Past tense form of 1st person, plural, negative"
    );
    T_EQ_ASSERT(
        "оқымадың",
        new VerbBuilder("оқу").pastForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Negative),
        "Past tense form of 2nd person, singular, negative"
    );
    T_EQ_ASSERT(
        "жаппадық",
        new VerbBuilder("жабу").pastForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Negative),
        "Past tense form of 1st person, plural, negative; special case"
    );
    T_EQ_ASSERT(
        "шықты",
        new VerbBuilder("шығу").pastForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Past tense form of 3rd person, singular, statement; consonant softening"
    );
    // TODO case with 'аңду'
}]);

ALL_TESTS.push(["pastTenseAllCasesTest", function() {
    testAllCases(
        "PastTense special case",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықтым", "қорықтық", "қорықтың", "қорықтыңдар", "қорықтыңыз", "қорықтыңыздар", "қорықты", "қорықты"]
    );
    testAllCases(
        "PastTense special case, negative",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпадым", "қорықпадық", "қорықпадың", "қорықпадыңдар", "қорықпадыңыз", "қорықпадыңыздар", "қорықпады", "қорықпады"]
    );
    testAllCases(
        "PastTense special case",
        "қорқу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықтым ба?", "қорықтық па?", "қорықтың ба?", "қорықтыңдар ма?", "қорықтыңыз ба?", "қорықтыңыздар ма?", "қорықты ма?", "қорықты ма?"]
    );

    testAllCases(
        "PastTense special case",
        "ірку",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["іріктім", "іріктік", "іріктің", "іріктіңдер", "іріктіңіз", "іріктіңіздер", "ірікті", "ірікті"]
    );
    testAllCases(
        "PastTense special case, negative",
        "ірку",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["ірікпедім", "ірікпедік", "ірікпедің", "ірікпедіңдер", "ірікпедіңіз", "ірікпедіңіздер", "ірікпеді", "ірікпеді"]
    );

    testAllCases(
        "PastTense special case",
        "тебу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["тептім бе?", "тептік пе?", "тептің бе?", "тептіңдер ме?", "тептіңіз бе?", "тептіңіздер ме?", "тепті ме?", "тепті ме?"]
    );
}]);

ALL_TESTS.push(["possibleFutureAllCasesTest", function() {
    testAllCases(
        "Possible future, special verb",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.possibleFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорқармын", "қорқармыз", "қорқарсың", "қорқарсыңдар", "қорқарсыз", "қорқарсыздар", "қорқар", "қорқар"]
    );
    testAllCases(
        "Possible future negative, special verb",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.possibleFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпаспын", "қорықпаспыз", "қорықпассың", "қорықпассыңдар", "қорықпассыз", "қорықпассыздар", "қорықпас", "қорықпас"]
    );
}]);

ALL_TESTS.push(["possibleFutureTenseTest", function() {
    T_EQ_ASSERT(
        "жазармын",
        new VerbBuilder("жазу").possibleFutureForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 1st person, singular, statement"
    );
    T_EQ_ASSERT(
        "айтарсың",
        new VerbBuilder("айту").possibleFutureForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 2nd person, singular, statement"
    );
    T_EQ_ASSERT(
        "көрер",
        new VerbBuilder("көру").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement"
    );
    T_EQ_ASSERT(
        "көрер",
        new VerbBuilder("көру").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement"
    );
    T_EQ_ASSERT(
        "демаларсыз",
        new VerbBuilder("демалу").possibleFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 2nd polite person, singular, statement"
    );
    T_EQ_ASSERT(
        "аударарсыздар",
        new VerbBuilder("аудару").possibleFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Plural, SentenceType.Statement),
        "Possible future form of 2nd polite person, plural, statement"
    );
    T_EQ_ASSERT(
        "келер",
        new VerbBuilder("келу").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement),
        "Possible future form of 3rd person, plural, statement"
    );
    T_EQ_ASSERT(
        "жаяр",
        new VerbBuilder("жаю").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement),
        "Possible future form of 3rd person, plural, statement; special case"
    );
    T_EQ_ASSERT(
        "қояр",
        new VerbBuilder("қою").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement; special case"
    );
    T_EQ_ASSERT(
        "сүйер",
        new VerbBuilder("сүю").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement; special case"
    );
    T_EQ_ASSERT(
        "оқыр",
        new VerbBuilder("оқу").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement; special case"
    );
    T_EQ_ASSERT(
        "ренжір",
        new VerbBuilder("ренжу").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Possible future form of 3rd person, singular, statement; special case"
    );
    T_EQ_ASSERT(
        "көрмес",
        new VerbBuilder("көру").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative),
        "Possible future form of 3rd person, singular, negative"
    );
    T_EQ_ASSERT(
        "көрмес",
        new VerbBuilder("көру").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Negative),
        "Possible future form of 3rd person, plural, negative"
    );
    T_EQ_ASSERT(
        "демалмассыз",
        new VerbBuilder("демалу").possibleFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Negative),
        "Possible future form of 2nd polite person, singular, negative"
    );
    T_EQ_ASSERT(
        "бармаспыз",
        new VerbBuilder("бару").possibleFutureForm(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Negative),
        "Possible future form of 1st person, plural, negative"
    );
    T_EQ_ASSERT(
        "жаппассыңдар",
        new VerbBuilder("жабу").possibleFutureForm(GrammarPerson.Second, GrammarNumber.Plural, SentenceType.Negative),
        "Possible future form of 2nd person, plural, negative; special case"
    );
    T_EQ_ASSERT(
        "теппес",
        new VerbBuilder("тебу").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Negative),
        "Possible future form of 3rd person, plural, negative; special case"
    );
}]);

ALL_TESTS.push(["possibleFutureTenseQuestionTest", function() {
    // No sources, just a guess.

    T_EQ_ASSERT(
        "жазармын ба?",
        new VerbBuilder("жазу").possibleFutureForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        "Possible future form of 1st person, singular, question"
    );
    T_EQ_ASSERT(
        "көрер ме?",
        new VerbBuilder("көру").possibleFutureForm(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        "Possible future form of 3rd person, singular, question"
    );
}]);

ALL_TESTS.push(["IntentionFutureTenseTest", function() {
    testAllCases(
        "Intention Future Tense",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбақпын", "жазбақпыз", "жазбақсың", "жазбақсыңдар", "жазбақсыз", "жазбақсыздар", "жазбақ", "жазбақ"]
    );
    testAllCases(
        "Intention Future Tense",
        "тебу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["теппекпін", "теппекпіз", "теппексің", "теппексіңдер", "теппексіз", "теппексіздер", "теппек", "теппек"]
    );
    testAllCases(
        "Intention Future Tense",
        "ішу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["ішпекпін", "ішпекпіз", "ішпексің", "ішпексіңдер", "ішпексіз", "ішпексіздер", "ішпек", "ішпек"]
    );
    testAllCases(
        "Intention Future Tense",
        "төлеу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["төлемекпін", "төлемекпіз", "төлемексің", "төлемексіңдер", "төлемексіз", "төлемексіздер", "төлемек", "төлемек"]
    );
    testAllCases(
        "Intention Future Tense",
        "оқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқымақпын", "оқымақпыз", "оқымақсың", "оқымақсыңдар", "оқымақсыз", "оқымақсыздар", "оқымақ", "оқымақ"]
    );
    testAllCases(
        "Intention Future Tense",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпақпын", "қорықпақпыз", "қорықпақсың", "қорықпақсыңдар", "қорықпақсыз", "қорықпақсыздар", "қорықпақ", "қорықпақ"]
    );
}]);

ALL_TESTS.push(["IntentionFutureNegativeTest", function() {
    testAllCases(
        "Regular negative of intention future",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбақ емеспін", "жазбақ емеспіз", "жазбақ емессің", "жазбақ емессіңдер", "жазбақ емессіз", "жазбақ емессіздер", "жазбақ емес", "жазбақ емес"]
    );
    testAllCases(
        "Tricky negative of intention future",
        "тебу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["теппек емеспін", "теппек емеспіз", "теппек емессің", "теппек емессіңдер", "теппек емессіз", "теппек емессіздер", "теппек емес", "теппек емес"]
    );
    testAllCases(
        "Regular negative of intention future",
        "ішу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["ішпек емеспін", "ішпек емеспіз", "ішпек емессің", "ішпек емессіңдер", "ішпек емессіз", "ішпек емессіздер", "ішпек емес", "ішпек емес"]
    );
    testAllCases(
        "Special negative of intention future",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпақ емеспін", "қорықпақ емеспіз", "қорықпақ емессің", "қорықпақ емессіңдер", "қорықпақ емессіз", "қорықпақ емессіздер", "қорықпақ емес", "қорықпақ емес"]
    );
}]);

ALL_TESTS.push(["IntentionFutureQuestionTest", function() {
    T_EQ_ASSERT(
        "ұйықтамақсыз ба?",
        new VerbBuilder("ұйықтау").intentionFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question),
        "Intention future form of 2nd polite person, singular, question"
    );
    T_EQ_ASSERT(
        "тамақтанбақсыз ба?",
        new VerbBuilder("тамақтану").intentionFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question),
        "Intention future form of 2nd polite person, singular, question"
    );
    T_EQ_ASSERT(
        "сөйлеспексің бе?",
        new VerbBuilder("сөйлесу").intentionFutureForm(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question),
        "Intention future form of 2nd person, singular, question"
    );
    T_EQ_ASSERT(
        "көмектеспексіздер ме?",
        new VerbBuilder("көмектесу").intentionFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Plural, SentenceType.Question),
        "Intention future form of 2nd person, plural, question"
    );
    T_EQ_ASSERT(
        "шешпексіз бе?",
        new VerbBuilder("шешу").intentionFutureForm(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question),
        "Intention future form of 2nd polite person, singular, question"
    );

    testAllCases(
        "Tricky question of intention future",
        "тебу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.intentionFutureForm(grammarPerson, grammarNumber, sentenceType);
        },
        ["теппекпін бе?", "теппекпіз бе?", "теппексің бе?", "теппексіңдер ме?", "теппексіз бе?", "теппексіздер ме?", "теппек пе?", "теппек пе?"]
    );
}]);

ALL_TESTS.push(["RemotePastAllCasesTest", function() {
    testAllCases(
        "Remote past, all cases",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазғанмын", "жазғанбыз", "жазғансың", "жазғансыңдар", "жазғансыз", "жазғансыздар", "жазған", "жазған"]
    );
    testAllCases(
        "Negative remote past, all cases",
        "жүзу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жүзгенмін", "жүзгенбіз", "жүзгенсің", "жүзгенсіңдер", "жүзгенсіз", "жүзгенсіздер", "жүзген", "жүзген"]
    );
    testAllCases(
        "Remote past, all cases, special verb",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорыққанмын", "қорыққанбыз", "қорыққансың", "қорыққансыңдар", "қорыққансыз", "қорыққансыздар", "қорыққан", "қорыққан"]
    );
    testAllCases(
        "Remote past, all cases, special verb",
        "ірку",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["іріккенмін", "іріккенбіз", "іріккенсің", "іріккенсіңдер", "іріккенсіз", "іріккенсіздер", "іріккен", "іріккен"]
    );
}]);

ALL_TESTS.push(["RemotePastNegativeAllCasesTest", function() {
    testAllCases(
        "Negative remote past, all cases",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазған жоқпын", "жазған жоқпыз", "жазған жоқсың", "жазған жоқсыңдар", "жазған жоқсыз", "жазған жоқсыздар", "жазған жоқ", "жазған жоқ"]
    );
    testAllCases(
        "Negative remote past, all cases",
        "жүзу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жүзген жоқпын", "жүзген жоқпыз", "жүзген жоқсың", "жүзген жоқсыңдар", "жүзген жоқсыз", "жүзген жоқсыздар", "жүзген жоқ", "жүзген жоқ"]
    );
    testAllCases(
        "Negative remote past, all cases, special verb",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорыққан жоқпын", "қорыққан жоқпыз", "қорыққан жоқсың", "қорыққан жоқсыңдар", "қорыққан жоқсыз", "қорыққан жоқсыздар", "қорыққан жоқ", "қорыққан жоқ"]
    );
    testAllCases(
        "Negative remote past, all cases, special verb",
        "ірку",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["іріккен жоқпын", "іріккен жоқпыз", "іріккен жоқсың", "іріккен жоқсыңдар", "іріккен жоқсыз", "іріккен жоқсыздар", "іріккен жоқ", "іріккен жоқ"]
    );
}]);

ALL_TESTS.push(["RemotePastQuestionAllCasesTest", function() {
    // No sources, just a guess.

    testAllCases(
        "Remote past, all cases",
        "жазу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазғанмын ба?", "жазғанбыз ба?", "жазғансың ба?", "жазғансыңдар ма?", "жазғансыз ба?", "жазғансыздар ма?", "жазған ба?", "жазған ба?"]
    );
    testAllCases(
        "Negative remote past, all cases",
        "жүзу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.remotePastTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жүзгенмін бе?", "жүзгенбіз бе?", "жүзгенсің бе?", "жүзгенсіңдер ме?", "жүзгенсіз бе?", "жүзгенсіздер ме?", "жүзген бе?", "жүзген бе?"]
    );
}]);

ALL_TESTS.push(["RemotePastTrickyCasesTest", function() {
    T_EQ_ASSERT(
        "жайған",
        new VerbBuilder("жаю").remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, base modification"
    );
    T_EQ_ASSERT(
        "қойған",
        new VerbBuilder("қою").remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, base modification"
    );
    T_EQ_ASSERT(
        "таныған",
        new VerbBuilder("тану", true).remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, base modification"
    );
    T_EQ_ASSERT(
        "оқыған",
        new VerbBuilder("оқу").remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, base modification"
    );
    T_EQ_ASSERT(
        "естігенмін",
        new VerbBuilder("есту").remotePastTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, base modification"
    );
    T_EQ_ASSERT(
        "естігенбіз",
        new VerbBuilder("есту").remotePastTense(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Statement),
        "Remote past, base modification"
    );
}]);

ALL_TESTS.push(["RemotePastExceptionalCasesTest", function() {
    T_EQ_ASSERT(
        "ашқан",
        new VerbBuilder("ашу").remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, variable base modification"
    );
    T_EQ_ASSERT(
        "ашыған",
        new VerbBuilder("ашу", true).remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, variable base modification"
    );
    T_EQ_ASSERT(
        "танған",
        new VerbBuilder("тану").remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, variable base modification"
    );
    T_EQ_ASSERT(
        "таныған",
        new VerbBuilder("тану", true).remotePastTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Remote past, variable base modification"
    );
}]);

ALL_TESTS.push(["PastUncertainAllCasesTest", function() {
    testAllCases(
        "Remote unwitnessed past, all cases",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазыппын", "жазыппыз", "жазыпсың", "жазыпсыңдар", "жазыпсыз", "жазыпсыздар", "жазыпты", "жазыпты"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "көру",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["көріппін", "көріппіз", "көріпсің", "көріпсіңдер", "көріпсіз", "көріпсіздер", "көріпті", "көріпті"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "ойнау",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["ойнаппын", "ойнаппыз", "ойнапсың", "ойнапсыңдар", "ойнапсыз", "ойнапсыздар", "ойнапты", "ойнапты"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "оқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқыппын", "оқыппыз", "оқыпсың", "оқыпсыңдар", "оқыпсыз", "оқыпсыздар", "оқыпты", "оқыпты"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "қою",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қойыппын", "қойыппыз", "қойыпсың", "қойыпсыңдар", "қойыпсыз", "қойыпсыздар", "қойыпты", "қойыпты"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "қорқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорқыппын", "қорқыппыз", "қорқыпсың", "қорқыпсыңдар", "қорқыпсыз", "қорқыпсыздар", "қорқыпты", "қорқыпты"]
    );
    testAllCases(
        "Remote unwitnessed past, all cases",
        "ірку",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["іркіппін", "іркіппіз", "іркіпсің", "іркіпсіңдер", "іркіпсіз", "іркіпсіздер", "іркіпті", "іркіпті"]
    );
}]);

ALL_TESTS.push(["PastUncertainNegativeAllCasesTest", function() {
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбаппын", "жазбаппыз", "жазбапсың", "жазбапсыңдар", "жазбапсыз", "жазбапсыздар", "жазбапты", "жазбапты"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "көру",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["көрмеппін", "көрмеппіз", "көрмепсің", "көрмепсіңдер", "көрмепсіз", "көрмепсіздер", "көрмепті", "көрмепті"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "ойнау",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["ойнамаппын", "ойнамаппыз", "ойнамапсың", "ойнамапсыңдар", "ойнамапсыз", "ойнамапсыздар", "ойнамапты", "ойнамапты"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "оқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқымаппын", "оқымаппыз", "оқымапсың", "оқымапсыңдар", "оқымапсыз", "оқымапсыздар", "оқымапты", "оқымапты"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "қою",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қоймаппын", "қоймаппыз", "қоймапсың", "қоймапсыңдар", "қоймапсыз", "қоймапсыздар", "қоймапты", "қоймапты"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "қорқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қорықпаппын", "қорықпаппыз", "қорықпапсың", "қорықпапсыңдар", "қорықпапсыз", "қорықпапсыздар", "қорықпапты", "қорықпапты"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "ірку",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["ірікпеппін", "ірікпеппіз", "ірікпепсің", "ірікпепсіңдер", "ірікпепсіз", "ірікпепсіздер", "ірікпепті", "ірікпепті"]
    );
    testAllCases(
        "Remote unwitnessed past, negative, all cases",
        "тебу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["теппеппін", "теппеппіз", "теппепсің", "теппепсіңдер", "теппепсіз", "теппепсіздер", "теппепті", "теппепті"]
    );
}]);

ALL_TESTS.push(["PastUncertainQuestionAllCasesTest", function() {
    testAllCases(
        "Remote unwitnessed past, question, all cases",
        "жазу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазыппын ба?", "жазыппыз ба?", "жазыпсың ба?", "жазыпсыңдар ма?", "жазыпсыз ба?", "жазыпсыздар ма?", "жазыпты ма?", "жазыпты ма?"]
    );
    testAllCases(
        "Remote unwitnessed past, question, all cases",
        "көру",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastUncertainTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["көріппін бе?", "көріппіз бе?", "көріпсің бе?", "көріпсіңдер ме?", "көріпсіз бе?", "көріпсіздер ме?", "көріпті ме?", "көріпті ме?"]
    );
}]);

ALL_TESTS.push(["pastUncertainReplaceUTest", function() {
    const relations = new Map([
        [new VerbSpec("жабу", false), "жауыппын"],
        [new VerbSpec("қабу", false), "қауыппын"],
        [new VerbSpec("қабу", true), "қабыппын"],
        [new VerbSpec("табу", false), "тауыппын"],
        [new VerbSpec("шабу", false), "шауыппын"],
        [new VerbSpec("кебу", false), "кеуіппін"],
        [new VerbSpec("себу", false), "сеуіппін"],
        [new VerbSpec("тебу", false), "теуіппін"],
        [new VerbSpec("өбу", false), "өбіппін"],
    ]);
    for (const [spec, form] of Array.from(relations.entries())) {
        T_EQ_ASSERT(
            form,
            new VerbBuilder(spec.verbDictForm, spec.forceExceptional).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
            "Past uncertain, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["pastUncertainNegativeReplaceUTest", function() {
    const relations = new Map([
        [new VerbSpec("жабу", false), "жаппаппын"],
        [new VerbSpec("қабу", false), "қаппаппын"],  // Example from internet: Ит Нұрбекті қаппапты
        [new VerbSpec("қабу", true), "қабымаппын"],
        [new VerbSpec("табу", false), "таппаппын"],
        [new VerbSpec("шабу", false), "шаппаппын"],
        [new VerbSpec("кебу", false), "кеппеппін"],
        [new VerbSpec("себу", false), "сеппеппін"],
        [new VerbSpec("тебу", false), "теппеппін"],
        [new VerbSpec("өбу", false), "өппеппін"],
    ]);
    for (const [spec, form] of Array.from(relations.entries())) {
        T_EQ_ASSERT(
            form,
            new VerbBuilder(spec.verbDictForm, spec.forceExceptional).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
            "Past uncertain, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["pastUncertainQuestionReplaceUTest", function() {
    const relations = new Map([
        [new VerbSpec("жабу", false), "жауыппын ба?"],
        [new VerbSpec("қабу", false), "қауыппын ба?"],
        [new VerbSpec("қабу", true), "қабыппын ба?"],
        [new VerbSpec("табу", false), "тауыппын ба?"],
        [new VerbSpec("шабу", false), "шауыппын ба?"],
        [new VerbSpec("кебу", false), "кеуіппін бе?"],
        [new VerbSpec("себу", false), "сеуіппін бе?"],
        [new VerbSpec("тебу", false), "теуіппін бе?"],
        [new VerbSpec("өбу", false), "өбіппін бе?"],
    ]);
    for (const [spec, form] of Array.from(relations.entries())) {
        T_EQ_ASSERT(
            form,
            new VerbBuilder(spec.verbDictForm, spec.forceExceptional).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
            "Past uncertain, b-to-u replacement: "
        );
    }
}]);

ALL_TESTS.push(["pastUncertainTrickyTest", function() {
    T_EQ_ASSERT(
        "ашыппын",
        new VerbBuilder("ашу").pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Past uncertain, tricky verb: "
    );
    // TODO check
    T_EQ_ASSERT(
        "ашпаппын",
        new VerbBuilder("ашу").pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        "Past uncertain, negative, tricky verb: "
    );
    T_EQ_ASSERT(
        "ашыппын ба?",
        new VerbBuilder("ашу").pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        "Past uncertain, question, tricky verb: "
    );
    T_EQ_ASSERT(
        "ашыппын",
        new VerbBuilder("ашу", true).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Past uncertain, tricky verb: "
    );
    T_EQ_ASSERT(
        "ашымаппын",
        new VerbBuilder("ашу", true).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        "Past uncertain, negative, tricky verb: "
    );
    T_EQ_ASSERT(
        "ашыппын ба?",
        new VerbBuilder("ашу", true).pastUncertainTense(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        "Past uncertain, question, tricky verb: "
    );
}]);

ALL_TESTS.push(["PastTransitiveAllCasesTest", function() {
    testAllCases(
        "Past transitive, all cases",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазатынмын", "жазатынбыз", "жазатынсың", "жазатынсыңдар", "жазатынсыз", "жазатынсыздар", "жазатын", "жазатын"]
    );
    testAllCases(
        "Past transitive, all cases",
        "көру",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["көретінмін", "көретінбіз", "көретінсің", "көретінсіңдер", "көретінсіз", "көретінсіздер", "көретін", "көретін"]
    );
    testAllCases(
        "Past transitive, all cases",
        "төлеу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["төлейтінмін", "төлейтінбіз", "төлейтінсің", "төлейтінсіңдер", "төлейтінсіз", "төлейтінсіздер", "төлейтін", "төлейтін"]
    );
    testAllCases(
        "Past transitive, all cases",
        "оқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқитынмын", "оқитынбыз", "оқитынсың", "оқитынсыңдар", "оқитынсыз", "оқитынсыздар", "оқитын", "оқитын"]
    );
    testAllCases(
        "Past transitive, all cases",
        "қою",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қоятынмын", "қоятынбыз", "қоятынсың", "қоятынсыңдар", "қоятынсыз", "қоятынсыздар", "қоятын", "қоятын"]
    );
}]);

ALL_TESTS.push(["PastTransitiveTrickyVerbsTest", function() {
    T_EQ_ASSERT(
        "танатын",
        new VerbBuilder("тану").pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Past transitive, variable base modification"
    );
    T_EQ_ASSERT(
        "танбайтын",
        new VerbBuilder("тану").pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative),
        "Past transitive, negative, variable base modification"
    );
    T_EQ_ASSERT(
        "танатын ба?",
        new VerbBuilder("тану").pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        "Past transitive, question, variable base modification"
    );
    T_EQ_ASSERT(
        "танитын",
        new VerbBuilder("тану", true).pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Statement),
        "Past transitive, variable base modification"
    );
    T_EQ_ASSERT(
        "танымайтын",
        new VerbBuilder("тану", true).pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Negative),
        "Past transitive, negative, variable base modification"
    );
    T_EQ_ASSERT(
        "танитын ба?",
        new VerbBuilder("тану", true).pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        "Past transitive, question, variable base modification"
    );
}]);

ALL_TESTS.push(["PastTransitiveNegativeAllCasesTest", function() {
    testAllCases(
        "Past transitive, all cases",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбайтынмын", "жазбайтынбыз", "жазбайтынсың", "жазбайтынсыңдар", "жазбайтынсыз", "жазбайтынсыздар", "жазбайтын", "жазбайтын"]
    );
    testAllCases(
        "Past transitive, all cases",
        "көру",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["көрмейтінмін", "көрмейтінбіз", "көрмейтінсің", "көрмейтінсіңдер", "көрмейтінсіз", "көрмейтінсіздер", "көрмейтін", "көрмейтін"]
    );
    testAllCases(
        "Past transitive, all cases",
        "төлеу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["төлемейтінмін", "төлемейтінбіз", "төлемейтінсің", "төлемейтінсіңдер", "төлемейтінсіз", "төлемейтінсіздер", "төлемейтін", "төлемейтін"]
    );
    testAllCases(
        "Past transitive, all cases",
        "оқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқымайтынмын", "оқымайтынбыз", "оқымайтынсың", "оқымайтынсыңдар", "оқымайтынсыз", "оқымайтынсыздар", "оқымайтын", "оқымайтын"]
    );
    testAllCases(
        "Past transitive, all cases",
        "қою",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["қоймайтынмын", "қоймайтынбыз", "қоймайтынсың", "қоймайтынсыңдар", "қоймайтынсыз", "қоймайтынсыздар", "қоймайтын", "қоймайтын"]
    );
    testAllCases(
        "Past transitive, all cases, tricky verb",
        "тебу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["теппейтінмін", "теппейтінбіз", "теппейтінсің", "теппейтінсіңдер", "теппейтінсіз", "теппейтінсіздер", "теппейтін", "теппейтін"]
    );
}]);

ALL_TESTS.push(["PastTransitiveQuestionAllCasesTest", function() {
    testAllCases(
        "Past transitive, all cases",
        "жазу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.pastTransitiveTense(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазатынмын ба?", "жазатынбыз ба?", "жазатынсың ба?", "жазатынсыңдар ма?", "жазатынсыз ба?", "жазатынсыздар ма?", "жазатын ба?", "жазатын ба?"]
    );

    T_EQ_ASSERT(
        "болатынсың ба?",
        new VerbBuilder("болу").pastTransitiveTense(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question),
        "Past transitive, regular, question"
    );
    T_EQ_ASSERT(
        "сөйлейтінсіз бе?",
        new VerbBuilder("сөйлеу").pastTransitiveTense(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Question),
        "Past transitive, regular, question"
    );
    T_EQ_ASSERT(
        "әкелетін бе?",
        new VerbBuilder("әкелу").pastTransitiveTense(GrammarPerson.Third, GrammarNumber.Singular, SentenceType.Question),
        "Past transitive, regular, question"
    );
}]);

ALL_TESTS.push(["ConditionalMoodAllCasesTest", function() {
    testAllCases(
        "Conditional mood, all cases",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазсам", "жазсақ", "жазсаң", "жазсаңдар", "жазсаңыз", "жазсаңыздар", "жазса", "жазса"]
    );
    testAllCases(
        "Conditional mood, all cases",
        "айту",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["айтсам", "айтсақ", "айтсаң", "айтсаңдар", "айтсаңыз", "айтсаңыздар", "айтса", "айтса"]
    );
    testAllCases(
        "Conditional mood, all cases",
        "көру",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["көрсем", "көрсек", "көрсең", "көрсеңдер", "көрсеңіз", "көрсеңіздер", "көрсе", "көрсе"]
    );
    testAllCases(
        "Conditional mood, all cases",
        "ішу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["ішсем", "ішсек", "ішсең", "ішсеңдер", "ішсеңіз", "ішсеңіздер", "ішсе", "ішсе"]
    );
    testAllCases(
        "Conditional mood, all cases",
        "оқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқысам", "оқысақ", "оқысаң", "оқысаңдар", "оқысаңыз", "оқысаңыздар", "оқыса", "оқыса"]
    );
    testAllCases(
        "Conditional mood, all cases",
        "есту",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["естісем", "естісек", "естісең", "естісеңдер", "естісеңіз", "естісеңіздер", "естісе", "естісе"]
    );
}]);

ALL_TESTS.push(["ConditionalMoodNegativeAllCasesTest", function() {
    testAllCases(
        "Conditional mood, negative, all cases",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбасам", "жазбасақ", "жазбасаң", "жазбасаңдар", "жазбасаңыз", "жазбасаңыздар", "жазбаса", "жазбаса"]
    );
    testAllCases(
        "Conditional mood, negative, all cases",
        "айту",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["айтпасам", "айтпасақ", "айтпасаң", "айтпасаңдар", "айтпасаңыз", "айтпасаңыздар", "айтпаса", "айтпаса"]
    );
    testAllCases(
        "Conditional mood, negative, all cases",
        "көру",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["көрмесем", "көрмесек", "көрмесең", "көрмесеңдер", "көрмесеңіз", "көрмесеңіздер", "көрмесе", "көрмесе"]
    );
    testAllCases(
        "Conditional mood, negative, all cases",
        "ішу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["ішпесем", "ішпесек", "ішпесең", "ішпесеңдер", "ішпесеңіз", "ішпесеңіздер", "ішпесе", "ішпесе"]
    );
    testAllCases(
        "Conditional mood, negative, all cases",
        "оқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқымасам", "оқымасақ", "оқымасаң", "оқымасаңдар", "оқымасаңыз", "оқымасаңыздар", "оқымаса", "оқымаса"]
    );
    testAllCases(
        "Conditional mood, negative, all cases",
        "есту",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["естімесем", "естімесек", "естімесең", "естімесеңдер", "естімесеңіз", "естімесеңіздер", "естімесе", "естімесе"]
    );
}]);

ALL_TESTS.push(["ConditionalMoodQuestionAllCasesTest", function() {
    // No sources, just a guess.

    testAllCases(
        "Conditional mood, all cases, question",
        "жазу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазсам ба?", "жазсақ па?", "жазсаң ба?", "жазсаңдар ма?", "жазсаңыз ба?", "жазсаңыздар ма?", "жазса ма?", "жазса ма?"]
    );
    testAllCases(
        "Conditional mood, all cases, question",
        "айту",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.conditionalMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["айтсам ба?", "айтсақ па?", "айтсаң ба?", "айтсаңдар ма?", "айтсаңыз ба?", "айтсаңыздар ма?", "айтса ма?", "айтса ма?"]
    );
}]);

ALL_TESTS.push(["ConditionalMoodSingleCasesTest", function() {
    T_EQ_ASSERT(
        "соқсам",
        new VerbBuilder("соғу").conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Conditional mood, tricky verb"
    );
    T_EQ_ASSERT(
        "қойсаңыз",
        new VerbBuilder("қою").conditionalMood(GrammarPerson.SecondPolite, GrammarNumber.Singular, SentenceType.Statement),
        "Conditional mood, tricky verb"
    );
}]);

ALL_TESTS.push(["ConditionalMoodTrickyVerbsTest", function() {
    T_EQ_ASSERT(
        "тансам",
        new VerbBuilder("тану").conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Conditional mood, tricky verb"
    );
    T_EQ_ASSERT(
        "танбасам",
        new VerbBuilder("тану").conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        "Conditional mood, tricky verb"
    );
    T_EQ_ASSERT(
        "тансам ба?",
        new VerbBuilder("тану").conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        "Conditional mood, tricky verb"
    );


    T_EQ_ASSERT(
        "танысам",
        new VerbBuilder("тану", true).conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Statement),
        "Conditional mood, tricky verb"
    );
    T_EQ_ASSERT(
        "танымасам",
        new VerbBuilder("тану", true).conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative),
        "Conditional mood, tricky verb"
    );
    T_EQ_ASSERT(
        "танысам ба?",
        new VerbBuilder("тану", true).conditionalMood(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Question),
        "Conditional mood, tricky verb"
    );
}]);

ALL_TESTS.push(["ImperativeMoodAllCasesTest", function() {
    testAllCases(
        "Imperative mood, all cases",
        "жазу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазайын", "жазайық", "жаз", "жазыңдар", "жазыңыз", "жазыңыздар", "жазсын", "жазсын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "беру",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["берейін", "берейік", "бер", "беріңдер", "беріңіз", "беріңіздер", "берсін", "берсін"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "төлеу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["төлейін", "төлейік", "төле", "төлеңдер", "төлеңіз", "төлеңіздер", "төлесін", "төлесін"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "оқу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқиын", "оқиық", "оқы", "оқыңдар", "оқыңыз", "оқыңыздар", "оқысын", "оқысын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "есту",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["естиін", "естиік", "есті", "естіңдер", "естіңіз", "естіңіздер", "естісін", "естісін"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "қою",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["қояйын", "қояйық", "қой", "қойыңдар", "қойыңыз", "қойыңыздар", "қойсын", "қойсын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "бағу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["бағайын", "бағайық", "бақ", "бағыңдар", "бағыңыз", "бағыңыздар", "бақсын", "бақсын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "тігу",
        SentenceType.Statement,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["тігейін", "тігейік", "тік", "тігіңдер", "тігіңіз", "тігіңіздер", "тіксін", "тіксін"]
    );
}]);

ALL_TESTS.push(["ImperativeMoodNegativeAllCasesTest", function() {
    testAllCases(
        "Imperative mood, negative, all cases",
        "жазу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазбайын", "жазбайық", "жазба", "жазбаңдар", "жазбаңыз", "жазбаңыздар", "жазбасын", "жазбасын"]
    );
    testAllCases(
        "Imperative mood, negative, all cases",
        "беру",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["бермейін", "бермейік", "берме", "бермеңдер", "бермеңіз", "бермеңіздер", "бермесін", "бермесін"]
    );
    testAllCases(
        "Imperative mood, negative, all cases",
        "төлеу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["төлемейін", "төлемейік", "төлеме", "төлемеңдер", "төлемеңіз", "төлемеңіздер", "төлемесін", "төлемесін"]
    );
    testAllCases(
        "Imperative mood, negative, all cases",
        "оқу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["оқымайын", "оқымайық", "оқыма", "оқымаңдар", "оқымаңыз", "оқымаңыздар", "оқымасын", "оқымасын"]
    );
    testAllCases(
        "Imperative mood, negative, all cases",
        "есту",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["естімейін", "естімейік", "естіме", "естімеңдер", "естімеңіз", "естімеңіздер", "естімесін", "естімесін"]
    );
    testAllCases(
        "Imperative mood, negative, all cases",
        "қою",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["қоймайын", "қоймайық", "қойма", "қоймаңдар", "қоймаңыз", "қоймаңыздар", "қоймасын", "қоймасын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "бағу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["бақпайын", "бақпайық", "бақпа", "бақпаңдар", "бақпаңыз", "бақпаңыздар", "бақпасын", "бақпасын"]
    );
    testAllCases(
        "Imperative mood, all cases",
        "тігу",
        SentenceType.Negative,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["тікпейін", "тікпейік", "тікпе", "тікпеңдер", "тікпеңіз", "тікпеңіздер", "тікпесін", "тікпесін"]
    );
}]);

ALL_TESTS.push(["ImperativeMoodQuestionAllCasesTest", function() {
    testAllCases(
        "Imperative mood, question, all cases",
        "жазу",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["жазайын ба?", "жазайық па?", "жаз ба?", "жазыңдар ма?", "жазыңыз ба?", "жазыңыздар ма?", "жазсын ба?", "жазсын ба?"]
    );
    testAllCases(
        "Imperative mood, question, all cases",
        "беру",
        SentenceType.Question,
        function(verbBuilder, grammarPerson, grammarNumber, sentenceType): Phrasal {
            return verbBuilder.imperativeMood(grammarPerson, grammarNumber, sentenceType);
        },
        ["берейін бе?", "берейік пе?", "бер ме?", "беріңдер ме?", "беріңіз бе?", "беріңіздер ме?", "берсін бе?", "берсін бе?"]
    );
}]);

ALL_TESTS.push(["ImperativeMoodSingleCasesTest", function() {
    T_EQ_ASSERT(
        "жаппа",
        new VerbBuilder("жабу").imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Negative),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "теппейік",
        new VerbBuilder("тебу").imperativeMood(GrammarPerson.First, GrammarNumber.Plural, SentenceType.Negative),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "соқсын",
        new VerbBuilder("соғу").imperativeMood(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Statement),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "соқсын ба?",
        new VerbBuilder("соғу").imperativeMood(GrammarPerson.Third, GrammarNumber.Plural, SentenceType.Question),
        "Imperative mood, tricky verb"
    );
}]);

ALL_TESTS.push(["ImperativeMoodTrickyVerbsTest", function() {
    T_EQ_ASSERT(
        "тан",
        new VerbBuilder("тану").imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Statement),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "танба",
        new VerbBuilder("тану").imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Negative),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "тан ба?",
        new VerbBuilder("тану").imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question),
        "Imperative mood, tricky verb"
    );


    T_EQ_ASSERT(
        "таны",
        new VerbBuilder("тану", true).imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Statement),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "таныма",
        new VerbBuilder("тану", true).imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Negative),
        "Imperative mood, tricky verb"
    );
    T_EQ_ASSERT(
        "таны ма?",
        new VerbBuilder("тану", true).imperativeMood(GrammarPerson.Second, GrammarNumber.Singular, SentenceType.Question),
        "Imperative mood, tricky verb"
    );
}]);

ALL_TESTS.push(["PastParticipleTest", function() {
    T_EQ_ASSERT(
        "айтқан",
        new VerbBuilder("айту").pastParticiple(SentenceType.Statement),
        "Past participle, statement"
    );
    T_EQ_ASSERT(
        "қайнаған",
        new VerbBuilder("қайнау").pastParticiple(SentenceType.Statement),
        "Past participle, statement"
    );
    T_EQ_ASSERT(
        "оқыған",
        new VerbBuilder("оқу").pastParticiple(SentenceType.Statement),
        "Past participle, statement"
    );
    T_EQ_ASSERT(
        "келген",
        new VerbBuilder("келу").pastParticiple(SentenceType.Statement),
        "Past participle, statement"
    );
}]);

ALL_TESTS.push(["PastParticipleNegativeTest", function() {
    T_EQ_ASSERT(
        "айтпаған",
        new VerbBuilder("айту").pastParticiple(SentenceType.Negative),
        "Past participle, negative"
    );
    T_EQ_ASSERT(
        "келмеген",
        new VerbBuilder("келу").pastParticiple(SentenceType.Negative),
        "Past participle, negative"
    );
    T_EQ_ASSERT(
        "ойламаған",
        new VerbBuilder("ойлау").pastParticiple(SentenceType.Negative),
        "Past participle, negative"
    );
}]);

ALL_TESTS.push(["PastParticipleQuestionTest", function() {
    T_EQ_ASSERT(
        "ауырған ба?",
        new VerbBuilder("ауыру").pastParticiple(SentenceType.Question),
        "Past participle, question"
    );
    T_EQ_ASSERT(
        "жазылған ба?",
        new VerbBuilder("жазылу").pastParticiple(SentenceType.Question),
        "Past participle, question"
    );
}]);

ALL_TESTS.push(["PresentParticipleTest", function() {
    T_EQ_ASSERT(
        "түсінетін",
        new VerbBuilder("түсіну").presentParticiple(SentenceType.Statement),
        "Present participle, statement"
    );
    T_EQ_ASSERT(
        "күлетін",
        new VerbBuilder("күлу").presentParticiple(SentenceType.Statement),
        "Present participle, statement"
    );
    T_EQ_ASSERT(
        "қарайтын",
        new VerbBuilder("қарау").presentParticiple(SentenceType.Statement),
        "Present participle, statement"
    );
    T_EQ_ASSERT(
        "жейтін",
        new VerbBuilder("жеу").presentParticiple(SentenceType.Statement),
        "Present participle, statement"
    );
}]);

ALL_TESTS.push(["PresentParticipleNegativeTest", function() {
    T_EQ_ASSERT(
        "келмейтін",
        new VerbBuilder("келу").presentParticiple(SentenceType.Negative),
        "Present participle, negative"
    );
    T_EQ_ASSERT(
        "айтпайтын",
        new VerbBuilder("айту").presentParticiple(SentenceType.Negative),
        "Present participle, negative"
    );
    T_EQ_ASSERT(
        "істемейтін",
        new VerbBuilder("істеу").presentParticiple(SentenceType.Negative),
        "Present participle, negative"
    );
    T_EQ_ASSERT(
        "болмайтын",
        new VerbBuilder("болу").presentParticiple(SentenceType.Negative),
        "Present participle, negative"
    );
}]);

ALL_TESTS.push(["PresentParticipleQuestionTest", function() {
    T_EQ_ASSERT(
        "түсінетін бе?",
        new VerbBuilder("түсіну").presentParticiple(SentenceType.Question),
        "Present participle, question"
    );
    T_EQ_ASSERT(
        "күлетін бе?",
        new VerbBuilder("күлу").presentParticiple(SentenceType.Question),
        "Present participle, question"
    );
    T_EQ_ASSERT(
        "қарайтын ба?",
        new VerbBuilder("қарау").presentParticiple(SentenceType.Question),
        "Present participle, question"
    );
    T_EQ_ASSERT(
        "жейтін бе?",
        new VerbBuilder("жеу").presentParticiple(SentenceType.Question),
        "Present participle, question"
    );
}]);

ALL_TESTS.push(["FutureParticipleTest", function() {
    const relations = [
        ["түсіну", "түсінер"],
        ["күлу", "күлер"],
        ["қарау", "қарар"],
        ["жеу", "жер"],
        ["қайту", "қайтар"],
        ["бару", "барар"],
        ["төлу", "төлер"],
    ];
    for (const [verb, expected] of relations) {
        T_EQ_ASSERT(
            expected,
            new VerbBuilder(verb).futureParticiple(SentenceType.Statement),
            "Future participle, statement"
        );
    }
}]);

ALL_TESTS.push(["FutureParticipleNegTest", function() {
    const relations = [
        ["келу", "келмес"],
        ["ойлау", "ойламас"],
        ["жету", "жетпес"],
        ["оқу", "оқымас"],
    ];
    for (const [verb, expected] of relations) {
        T_EQ_ASSERT(
            expected,
            new VerbBuilder(verb).futureParticiple(SentenceType.Negative),
            "Future participle, negative"
        );
    }
}]);

ALL_TESTS.push(["FutureParticipleQuestionTest", function() {
    const relations = [
        ["түсіну", "түсінер ме?"],
        ["күлу", "күлер ме?"],
        ["қарау", "қарар ма?"],
        ["жеу", "жер ме?"],
        ["қайту", "қайтар ма?"],
        ["бару", "барар ма?"],
        ["төлу", "төлер ме?"],
    ];
    for (const [verb, expected] of relations) {
        T_EQ_ASSERT(
            expected,
            new VerbBuilder(verb).futureParticiple(SentenceType.Question),
            "Future participle, question"
        );
    }
}]);

/* End of tests */

testAll();
