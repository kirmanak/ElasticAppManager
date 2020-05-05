package ru.ifmo.kirmanak.manager.controllers

import org.springframework.data.repository.CrudRepository
import ru.ifmo.kirmanak.manager.storage.entities.OpenNebulaConfigEntity

interface OpenNebulaConfigRepository : CrudRepository<OpenNebulaConfigEntity, Long>