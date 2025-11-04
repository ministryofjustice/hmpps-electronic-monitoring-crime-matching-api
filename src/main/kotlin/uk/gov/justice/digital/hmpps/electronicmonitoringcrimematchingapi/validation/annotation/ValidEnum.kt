package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.validation.annotation

import com.nimbusds.jose.Payload
import jakarta.validation.Constraint
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.validation.validator.EnumValidator
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
  val enumClass: KClass<out Enum<*>>,
  val message: String = "Must be any of enum {enumClass}",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
