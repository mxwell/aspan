function test001() {
    let verbs = ["алу", "беру"];
    for (const verb of verbs) {
        let verbBuilder = new VerbBuilder(verb);
        console.log("base: " + verbBuilder.verb_base);
        console.log(
            "form: " + verbBuilder.present_transitive_form(
                "First",
                "Singular",
                "Statement"
            )
        );
    }
}

test001();