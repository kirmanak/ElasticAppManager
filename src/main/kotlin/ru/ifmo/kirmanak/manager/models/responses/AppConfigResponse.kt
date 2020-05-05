package ru.ifmo.kirmanak.manager.models.responses

data class AppConfigResponse(
        val openNebulaApps: List<OpenNebulaConfigResponse>,
        val kubernetesApps: List<KubernetesConfigResponse>
)