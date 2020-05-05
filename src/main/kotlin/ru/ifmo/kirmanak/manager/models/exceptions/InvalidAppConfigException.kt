package ru.ifmo.kirmanak.manager.models.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Application configuration is invalid")
class InvalidAppConfigException(override val message: String) : Exception(message) {
    constructor(e: Throwable) : this(e.message ?: e.toString()) {
        addSuppressed(e)
    }
}