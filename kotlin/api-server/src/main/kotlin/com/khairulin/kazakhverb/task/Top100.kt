package com.khairulin.kazakhverb.task

import io.ktor.util.logging.KtorSimpleLogger

object Top100 {
    private val LOG = KtorSimpleLogger("Top100")

    private val verbs: List<VerbEntry> by lazy {
        readEntries()
    }

    private fun readEntries(): List<VerbEntry> {
        val entries = mutableListOf<VerbEntry>()

        val resourcePath = "/top100.colonsv"
        val inputStream = Top100.javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: ${resourcePath}")

        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val parts = line.split(":")
                if (parts.size >= 12) {
                    val verbDictForm = parts[0]
                    val forceExceptional = parts[1] == "1"
                    val preceding = if (parts[11].isNotEmpty()) {
                        parts[11].split(";").map { it.trim() }
                    } else {
                        emptyList()
                    }

                    entries.add(
                        VerbEntry(
                            verbDictForm = verbDictForm,
                            forceExceptional = forceExceptional,
                            preceding = preceding
                        )
                    )
                }
            }
        }

        LOG.info("loaded ${entries.size} verbs")

        return entries
    }

    fun pickRandom() = verbs.random()
}