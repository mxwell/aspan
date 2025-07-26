package com.khairulin.kazakhverb.latex

import java.io.FileWriter

fun main(args: Array<String>) {
    val verbs = VerbList().loadList()

    val latexGenerator = LatexGenerator()

    val outputFile = FileWriter("output.tex")
    outputFile.buffered().apply {
        write(latexGenerator.generate(verbs))
        close()
    }
}