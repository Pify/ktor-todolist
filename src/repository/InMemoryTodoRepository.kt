package com.pify.repository

import com.pify.entities.Todo
import com.pify.entities.TodoDraft

class InMemoryTodoRepository: TodoRepository {

    private val todos = mutableListOf<Todo>()

    override fun getAllTodos(): List<Todo> {
        return todos
    }

    override fun getTodo(id: Int): Todo? {
        return todos.firstOrNull{ it.id == id}
    }

    override fun addTodo(draft: TodoDraft): Todo {
        val todo = Todo(
            id = todos.size + 1,
            title = draft.title,
            done = draft.done,
        )
        todos.add(todo)
        return todo
    }

    override fun removeTodo(id: Int): Boolean {
        return todos.removeIf { it.id == id}
    }

    override fun updateTodo(id: Int, draft: TodoDraft): Boolean {
        val todo = todos.firstOrNull { it.id == id }
            ?: return false

        todo.apply {
            title = draft.title
            done = draft.done
        }
        return true
    }
}