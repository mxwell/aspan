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
    let verbDictForm = "ренжу";
    let verbBuilder = new VerbBuilder(verbDictForm);
    T_EQ_ASSERT("ренжимеймін", verbBuilder.presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative), "Tricky negative form: ");
}]);

ALL_TESTS.push(["testVerbSounds", function() {
    let verbBuilder = new VerbBuilder("тігу");
    let s = verbBuilder.presentTransitiveForm(GrammarPerson.First, GrammarNumber.Singular, SentenceType.Negative);
    T_EQ_ASSERT("тікпеймін", s, "Bad sounding consonant bigrams must be fixed: ");
}]);

/* End of tests */

testAll();