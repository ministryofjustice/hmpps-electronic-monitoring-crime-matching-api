package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

abstract class Expression<T> {
  abstract fun parameters(): List<String>
}
