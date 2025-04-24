package com.khairulin.kazakhverb.task

data class VerbEntry(
    val verbDictForm: String,
    val forceExceptional: Boolean = false,
    val preceding: List<String> = emptyList(),
) {
    fun randomPreceding(): String {
        if (preceding.isEmpty()) {
            return ""
        }
        return preceding.random()
    }
}
