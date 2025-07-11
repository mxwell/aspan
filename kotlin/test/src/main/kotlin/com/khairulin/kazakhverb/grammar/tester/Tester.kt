package com.khairulin.kazakhverb.grammar.tester

fun main(args: Array<String>) {
    assert(args.size >= 2) {
        "not enough args: ${args.size}"
    }

    val command = args[0]
    if (command == "Adj") {
        AdjTest().test()
    } else if (command == "Conjugation") {
        ConjugationTest().test()
    } else if (command == "DatasetConjugation") {
        val arg = args[1]
        DatasetConjugationTest(arg).test()
    } else if (command == "DatasetDeclension") {
        val arg = args[1]
        DatasetDeclensionTest(arg).test()
    } else if (command == "Declension") {
        DeclensionTest().test()
    } else {
        println("Unknown command ${command}")
    }
}