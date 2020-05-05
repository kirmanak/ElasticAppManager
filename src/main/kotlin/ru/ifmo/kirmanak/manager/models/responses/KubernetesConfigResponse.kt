package ru.ifmo.kirmanak.manager.models.responses

data class KubernetesConfigResponse(
        val appId: Long,
        val namespace: String,
        val deployment: String
)
