package com.khairulin.kazakhverb.plugins

import com.khairulin.kazakhverb.ApiServer
import com.khairulin.kazakhverb.response.GetTopics
import com.khairulin.kazakhverb.task.TaskTopic
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(apiServer: ApiServer) {
    routing {
        get("/gymapi") {
            call.respondText("Have you been to the gym today?")
        }
        get("/gymapi/v1/get_topics") {
            val topics = TaskTopic.entries
            val ruTitles = topics.map { it.ruTitle }
            call.respond(GetTopics(topics, ruTitles))
        }
        get("/gymapi/v1/get_tasks") {
            val topicName = call.parameters["topic"]
            val topic = TaskTopic.of(topicName)
            if (topic == null) {
                call.respondText(
                    "invalid topic",
                    status = HttpStatusCode.BadRequest
                )
                return@get
            }
            val tasks = apiServer.generateTasks(topic)
            if (tasks == null) {
                call.respondText(
                    "failed to generate tasks",
                    status = HttpStatusCode.InternalServerError
                )
                return@get
            }
            call.respond(tasks)
        }
    }
}