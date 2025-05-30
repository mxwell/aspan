package com.khairulin.kazakhverb.grammar.tester

fun main(args: Array<String>) {
    assert(args.size >= 2) {
        "not enough args: ${args.size}"
    }

    val command = args[0]
    if (command == "Conjugation") {
        ConjugationTest().test()
    } else if (command == "Declension") {
        val arg = args[1]
        DeclensionTest(arg).test()
    } else {
        println("Unknown command ${command}")
    }
}