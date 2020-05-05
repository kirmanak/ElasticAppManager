package ru.ifmo.kirmanak.manager.models.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class NoSuchApplicationException(id: Long) : Exception("Application with id = $id was not found")