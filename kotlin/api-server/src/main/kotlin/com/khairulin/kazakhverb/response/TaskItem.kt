package com.khairulin.kazakhverb.response

import kotlinx.serialization.Serializable

@Serializable
data class TaskItem(
    val question: String,
    val answers: List<String>,
    val translations: List<Pair<String, String>> = emptyList(),
)