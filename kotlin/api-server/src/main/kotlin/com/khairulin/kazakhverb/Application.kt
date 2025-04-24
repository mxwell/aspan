package com.khairulin.kazakhverb

import com.khairulin.kazakhverb.plugins.configureRouting
import com.khairulin.kazakhverb.plugins.configureSerialization
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val apiServer = ApiServer()
    configureRouting(apiServer)
    configureSerialization()
}
