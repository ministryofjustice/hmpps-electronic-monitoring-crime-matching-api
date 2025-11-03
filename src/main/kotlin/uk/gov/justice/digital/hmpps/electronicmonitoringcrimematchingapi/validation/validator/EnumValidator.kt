package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.validation.validator

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.validation.annotation.ValidEnum

class EnumValidator : ConstraintValidator<ValidEnum, String> {
  private lateinit var acceptedValues: Set<String>

  override fun initialize(annotation: ValidEnum) {
    acceptedValues = annotation.enumClass.java.enumConstants.map { it.name }.toSet()
  }

  override fun isValid(value: String?, context: ConstraintValidatorContext?) = acceptedValues.contains(value)
}
