package com.khairulin.kazakhverb.task

object TaskDescription {
    fun compose(label: String, pattern: String): String {
        return "(${label})\n\n*${pattern}*"
    }
}