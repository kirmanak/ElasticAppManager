package ru.ifmo.kirmanak.manager.controllers

import org.springframework.data.repository.CrudRepository
import ru.ifmo.kirmanak.manager.storage.entities.KubernetesConfigEntity

interface KubernetesConfigRepository : CrudRepository<KubernetesConfigEntity, Long> {
    fun findByNamespaceAndDeploymentAndYaml(namespace: String, deployment: String, yaml: String): KubernetesConfigEntity?
}