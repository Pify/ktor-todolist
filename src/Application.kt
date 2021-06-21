package com.pify

import com.pify.authentication.JwtConfig
import com.pify.entities.LoginBody
import com.pify.entities.TodoDraft
import com.pify.repository.InMemoryUserRepository
import com.pify.repository.MySqlTodoRepository
import com.pify.repository.TodoRepository
import com.pify.repository.UserRepository
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val jwtConfig = JwtConfig(System.getenv("JWT_SECRET"))

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(Authentication) {
        jwt {
            jwtConfig.configureKtorFeature(this)
        }
    }

    routing {

        val repository: TodoRepository = MySqlTodoRepository()
        val userRepository: UserRepository = InMemoryUserRepository()

        get("/") {
            call.respondText("Hello TodoList!")
        }

        post("/login") {
            val loginBody = call.receive<LoginBody>()

            val user = userRepository.getUser(loginBody.username, loginBody.password)

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials !")
                return@post
            }

            val token = jwtConfig.genereateToken(
                JwtConfig.JwtUser(user.userId, user.username)
            )
            call.respond(token)
        }

        authenticate {
            get("/me") {
                val user = call.authentication.principal as JwtConfig.JwtUser
                call.respond(user)
            }

            get("/todos") {
                //will return all todolist
                call.respond(repository.getAllTodos())
            }

            get("/todos/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "id parameter has to be a number")
                    return@get
                }

                val todo = repository.getTodo(id)

                if (todo == null) {
                    call.respond(HttpStatusCode.NotFound, "found no todo for the provided id $id")
                } else {
                    call.respond(todo)
                }
            }

            post("/todos") {
                //post todolist
                val parameters = call.receiveParameters()
                val todoDraft = TodoDraft(parameters["title"].toString(), parameters["done"].toBoolean())

                val todo = repository.addTodo(todoDraft)
                call.respond(todo)
            }

            put("/todos/{id}") {
                //edit single todolist
                val todoDraft = call.receive<TodoDraft>()
                val todoId = call.parameters["id"]?.toIntOrNull()

                if (todoId == null) {
                    call.respond(HttpStatusCode.BadRequest, "id parameter has to be a number!")
                    return@put
                }

                val updated = repository.updateTodo(todoId, todoDraft)
                if (updated) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "found no todo with the id $todoId"
                    )
                }
            }

            delete("/todos/{id}") {
                //delete single todolist
                val todoId = call.parameters["id"]?.toIntOrNull()

                if (todoId == null) {
                    call.respond(HttpStatusCode.BadRequest, "id parameter has to be a number!")
                    return@delete
                }

                val removed = repository.removeTodo(todoId)
                if (removed) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "found no todo with the id $todoId"
                    )
                }
            }
        }

        var fileDescription = ""
        var fileName = ""
        post("/upload") {
            val multipartData = call.receiveMultipart()
            val params = call.receiveParameters()

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        fileDescription = part.value
                    }
                    is PartData.FileItem -> {
                        fileName = part.originalFileName as String
                        var filebytes = part.streamProvider().readBytes()
                        File("uploads/$fileName").writeBytes(filebytes)
                    }
                }
            }

            call.respondText("$fileDescription is uploaded to 'uploads/$fileName', data : ${params["userId"]}")
        }
    }
}

