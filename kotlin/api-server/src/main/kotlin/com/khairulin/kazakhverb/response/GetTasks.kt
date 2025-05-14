package com.khairulin.kazakhverb.response

import kotlinx.serialization.Serializable

@Serializable
data class GetTasks(
    val tasks: List<TaskItem>,
    val references: List<TheoryReference> = emptyList()
)
