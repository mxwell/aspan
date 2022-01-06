function test001() {
    let verbs = ["алу", "беру"];
    for (const verb of verbs) {
        let verbBuilder = new VerbBuilder(verb);
        console.log("base: " + verbBuilder.verb_base);
        console.log(
            "form: " + verbBuilder.presentTransitiveForm(
                GrammarPerson.First,
                GrammarNumber.Singular,
                SentenceType.Statement
            )
        );
        console.log(
            "negative form: " + verbBuilder.presentTransitiveForm(
                GrammarPerson.Second,
                GrammarNumber.Plural,
                SentenceType.Negative
            )
        );
        console.log(
            "question form: " + verbBuilder.presentTransitiveForm(
                GrammarPerson.First,
                GrammarNumber.Singular,
                SentenceType.Question
            )
        );
    }
}

test001();