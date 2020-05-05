package ru.ifmo.kirmanak.manager.storage.constraints

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Constraint(validatedBy = [OnlyOneSetValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnlyOneSetConstraint(
        val groups: Array<KClass<in Any>> = [],
        val payload: Array<KClass<in Payload>> = [],
        val fields: Array<String> = [],
        val message: String = "Only one field must be not-null"
)