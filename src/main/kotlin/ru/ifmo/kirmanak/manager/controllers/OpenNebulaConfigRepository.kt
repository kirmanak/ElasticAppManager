package ru.ifmo.kirmanak.manager.controllers

import org.springframework.data.repository.CrudRepository
import ru.ifmo.kirmanak.manager.storage.entities.OpenNebulaConfigEntity

interface OpenNebulaConfigRepository : CrudRepository<OpenNebulaConfigEntity, Long> {
    fun findByAddressAndLoginAndPasswordAndRoleAndTemplateAndVmgroup(
            address: String, login: String, password: String, role: Int, template: Int, vmgroup: Int
    ): OpenNebulaConfigEntity?
}