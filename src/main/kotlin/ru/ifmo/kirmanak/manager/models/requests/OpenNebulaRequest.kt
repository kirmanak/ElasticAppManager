package ru.ifmo.kirmanak.manager.models.requests

data class OpenNebulaRequest(
        val address: String,
        val login: String,
        val password: String,
        val role: Int,
        val template: Int,
        val vmgroup: Int
)