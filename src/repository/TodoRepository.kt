package com.pify.repository

import com.pify.entities.Todo
import com.pify.entities.TodoDraft

interface TodoRepository {

    fun getAllTodos(): List<Todo>

    fun getTodo(id: Int): Todo?

    fun addTodo(draft: TodoDraft): Todo

    fun removeTodo(id: Int): Boolean

    fun updateTodo(id: Int, draft: TodoDraft): Boolean
}