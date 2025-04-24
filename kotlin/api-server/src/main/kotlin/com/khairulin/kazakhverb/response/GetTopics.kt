package com.khairulin.kazakhverb.response

import com.khairulin.kazakhverb.task.TaskTopic
import kotlinx.serialization.Serializable

@Serializable
data class GetTopics(
    val topics: List<TaskTopic>
)
