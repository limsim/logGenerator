package com.shc

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.client.*
import io.ktor.client.engine.jetty.Jetty
import io.ktor.client.features.logging.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.features.*
import org.slf4j.event.*
import io.ktor.server.engine.*
import io.ktor.util.error
import io.ktor.util.pipeline.PipelineContext
import org.eclipse.jetty.util.ssl.SslContextFactory
import kotlin.Exception

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val client = HttpClient(engineFactory = Jetty) {
        engine {
            sslContextFactory = SslContextFactory()
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }

    install(Locations) {
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ShutDownUrl.ApplicationCallFeature) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = "/ktor/application/shutdown"
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        suspend fun PipelineContext<Unit, ApplicationCall>.exceptionChucker(exception: Exception) {
            try {
                throw exception
            } catch (e: Exception) {
                log.error(e)
                call.respondText(e.toString(), ContentType.Any, HttpStatusCode.InternalServerError)
            }
        }

        suspend fun PipelineContext<Unit, ApplicationCall>.errorChucker(error: Error) {
            try {
                throw error
            } catch (e: Error) {
                log.error(e)
                call.respondText(e.toString(), ContentType.Any, HttpStatusCode.InternalServerError)
            }
        }

        get("/IllegalArgumentException") {
            exceptionChucker(IllegalArgumentException())
        }

        get("/IndexOutOfBoundsException") {
            exceptionChucker(IndexOutOfBoundsException())
        }

        get("/NullPointerException") {
            exceptionChucker(NullPointerException())
        }

        get("/NumberFormatException") {
            exceptionChucker(NumberFormatException())
        }

        get("/OutOfMemoryError") {
            errorChucker(OutOfMemoryError())
        }

        get("/OK") {
            statusCodePrinter(HttpStatusCode.OK)
        }

        get("/NotFound") {
            statusCodePrinter(HttpStatusCode.NotFound)
        }

        get("/InternalServerError") {
            statusCodePrinter(HttpStatusCode.InternalServerError)
        }

        get("/MovedPermanently") {
            statusCodePrinter(HttpStatusCode.MovedPermanently)
        }

        get("/BadRequest") {
            statusCodePrinter(HttpStatusCode.BadRequest)
        }

        get("/Unauthorized") {
            statusCodePrinter(HttpStatusCode.Unauthorized)
        }

        get("/Forbidden") {
            statusCodePrinter(HttpStatusCode.Forbidden)
        }

        get("/BadGateway") {
            statusCodePrinter(HttpStatusCode.BadGateway)
        }

        get("/ServiceUnavailable") {
            statusCodePrinter(HttpStatusCode.ServiceUnavailable)
        }

        get("/GatewayTimeout") {
            statusCodePrinter(HttpStatusCode.GatewayTimeout)
        }

        post("/create") {
            statusCodePrinter(HttpStatusCode.Created)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        get<MyLocation> {
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            call.respondText("Inside $it")
        }
        get<Type.List> {
            call.respondText("Inside $it")
        }

        get<Journey> {
            when(it.page) {
                2 -> statusCodePrinter(HttpStatusCode.NotFound)
                3 -> statusCodePrinter(HttpStatusCode.InternalServerError)
                4 -> statusCodePrinter(HttpStatusCode.MovedPermanently)
                5 -> statusCodePrinter(HttpStatusCode.BadRequest)
                6 -> statusCodePrinter(HttpStatusCode.Unauthorized)
                7 -> statusCodePrinter(HttpStatusCode.Forbidden)
                8 -> statusCodePrinter(HttpStatusCode.BadGateway)
                9 -> statusCodePrinter(HttpStatusCode.ServiceUnavailable)
                10 -> statusCodePrinter(HttpStatusCode.GatewayTimeout)
                11 -> statusCodePrinter(HttpStatusCode.Created)
                12 -> exceptionChucker(IllegalArgumentException())
                13 -> exceptionChucker(IndexOutOfBoundsException())
                14 -> exceptionChucker(NullPointerException())
                15 -> exceptionChucker(NumberFormatException())
                16 -> errorChucker(OutOfMemoryError())
                else -> { statusCodePrinter(HttpStatusCode.OK) }
            }
        }

        install(StatusPages) {
            exception<AuthenticationException> { cause ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.statusCodePrinter(statusCode: HttpStatusCode) {
    call.respondText("This is the status code: $statusCode", ContentType.Any, statusCode)
}

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")

@Location("/type/{name}") data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}

@Location("myjourney/{page}")
data class Journey(val page: Int)

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

