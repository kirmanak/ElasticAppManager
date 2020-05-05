package ru.ifmo.kirmanak.manager.storage.constraints

import org.springframework.beans.BeanWrapperImpl
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class OnlyOneSetValidator : ConstraintValidator<OnlyOneSetConstraint, Any> {
    private lateinit var columns: Array<String>

    override fun initialize(constraintAnnotation: OnlyOneSetConstraint) {
        super.initialize(constraintAnnotation)
        columns = constraintAnnotation.fields
    }

    override fun isValid(value: Any, context: ConstraintValidatorContext): Boolean {
        val wrapper = BeanWrapperImpl(value)

        return columns.map { wrapper.getPropertyValue(it) }.count { it != null } == 1
    }

}
