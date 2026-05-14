package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

abstract class Expression {
  abstract fun parameters(): List<String>
}
