package ru.ifmo.kirmanak.manager.models.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Can not establish connection to the requested application")
class NoAppConnectionException(override val message: String) : Exception(message) {
    constructor(e: Throwable) : this(e.message ?: e.toString()) {
        addSuppressed(e)
    }
}