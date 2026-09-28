package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class PagedResult<T>(
  val data: List<T>,
  val pageCount: Int,
)
