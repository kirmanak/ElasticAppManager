package ru.ifmo.kirmanak.manager.models.responses

data class OpenNebulaConfigResponse(
        val appId: Long,
        val address: String,
        val role: Int,
        val template: Int,
        val vmgroup: Int
)
