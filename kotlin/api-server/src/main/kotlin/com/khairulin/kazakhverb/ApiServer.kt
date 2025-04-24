package com.khairulin.kazakhverb

import com.khairulin.kazakhverb.response.GetTasks
import com.khairulin.kazakhverb.task.TaskGenerator
import com.khairulin.kazakhverb.task.TaskTopic

class ApiServer {
    private val taskGenerator = TaskGenerator()

    fun generateTasks(topic: TaskTopic): GetTasks? {
        return taskGenerator.generateTopicTasks(topic)
    }
}