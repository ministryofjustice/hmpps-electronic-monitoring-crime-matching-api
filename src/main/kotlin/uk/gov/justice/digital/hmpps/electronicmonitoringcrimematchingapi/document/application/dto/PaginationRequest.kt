package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException

data class PaginationRequest(
  val page: Int,
  val size: Int,
) {
  init {
    if (page < 0) {
      throw InvalidRequestException("Page must not be negative")
    }

    if (size !in 1..100) {
      throw InvalidRequestException("Size must be between 1 and 100")
    }
  }
}
