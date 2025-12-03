package com.example

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.reviewRouter()
{
    route("/review")
    {
        post()
        {
            //pass
        }
    }
}