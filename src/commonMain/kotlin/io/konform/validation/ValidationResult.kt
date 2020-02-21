package io.konform.validation

import kotlin.reflect.KProperty1

interface ValidationErrorMessage {
    val dataPath: String
    val message: String
}

internal data class PropertyValidationErrorMessage(
    override val dataPath: String,
    override val message: String
) : ValidationErrorMessage

interface ValidationErrors : List<ValidationErrorMessage>

internal object NoValidationErrors : ValidationErrors, List<ValidationErrorMessage> by emptyList()
internal class DefaultValidationErrors(private val errors: List<ValidationErrorMessage>) : ValidationErrors, List<ValidationErrorMessage> by errors {
    override fun toString(): String {
        return errors.toString()
    }
}

sealed class ValidationResult<T> {
    abstract operator fun get(vararg propertyPath: Any): List<String>?
    abstract fun <R> map(transform: (T) -> R): ValidationResult<R>
    abstract val errors: ValidationErrors
}

data class Invalid<T>(
    internal val internalErrors: Map<String, List<String>>) : ValidationResult<T>() {

    override fun get(vararg propertyPath: Any): List<String>? =
        internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Invalid(this.internalErrors)

    private fun toPathSegment(it: Any): String {
        return when (it) {
            is KProperty1<*, *> -> ".${it.name}"
            is Int -> "[$it]"
            else -> ".$it"
        }
    }

    override val errors: ValidationErrors by lazy {
        DefaultValidationErrors(
            internalErrors.flatMap { (path, errors ) ->
                errors.map { PropertyValidationErrorMessage(path, it) }
            }
        )
    }
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Valid(transform(this.value))
    override val errors: ValidationErrors
        get() = DefaultValidationErrors(emptyList())
}