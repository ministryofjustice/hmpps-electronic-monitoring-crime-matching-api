package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

data class PageResult<T>(
  val pageNumber: Int,
  val totalPages: Int,
  val results: List<T>
)