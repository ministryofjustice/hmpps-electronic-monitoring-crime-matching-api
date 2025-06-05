package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

data class PageResult<T>(
  val page: Int,
  val totalPages: Int,
  val results: List<T>
)