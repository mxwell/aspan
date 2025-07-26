package com.khairulin.kazakhverb.latex

import org.slf4j.LoggerFactory

class VerbList {
    val LOG = LoggerFactory.getLogger(this.javaClass)

    fun loadList(): List<VerbEntry> {
        val entries = mutableListOf<VerbEntry>()

        val resourcePath = "/kaztili_top100.colonsv"
        val inputStream = this.javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: ${resourcePath}")

        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 2) {
                    val verbDictForm = parts[0]
                    val forceExceptional = parts[1] == "1"

                    entries.add(
                        VerbEntry(
                            verbDictForm = verbDictForm,
                            forceExceptional = forceExceptional,
                        )
                    )
                }
            }
        }

        LOG.info("loaded ${entries.size} verbs")

        return entries
    }
}