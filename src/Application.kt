package com.pify

import com.pify.entities.Todo
import com.pify.entities.TodoDraft
import com.pify.repository.InMemoryTodoRepository
import com.pify.repository.MySqlTodoRepository
import com.pify.repository.TodoRepository
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {

        val repository: TodoRepository = MySqlTodoRepository()

        get("/") {
            call.respondText("Hello TodoList!")
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
            val todoDraft = call.receive<TodoDraft>() //receive json in shape of TodoDraft
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
}

