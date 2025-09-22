package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import com.fasterxml.jackson.databind.PropertyNamingStrategies

class AlphanumericSnakeCaseStrategy : PropertyNamingStrategies.NamingBase() {
  override fun translate(input: String?): String? = if (input == null) {
    null
  } else {
    "([A-Z]+|[0-9]+)".toRegex().replace(input) { "_${it.groupValues[1]}".lowercase() }
  }
}
