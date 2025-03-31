package org.example

object StrManip {
    fun getLastWord(string: String): String {
        val spaceIndex = string.lastIndexOf(" ")
        return if (spaceIndex != -1) {
            string.substring(spaceIndex + 1)
        } else {
            string
        }
    }

    fun replaceLast(string: String, char: Char): String {
        return string.dropLast(1) + char
    }
}