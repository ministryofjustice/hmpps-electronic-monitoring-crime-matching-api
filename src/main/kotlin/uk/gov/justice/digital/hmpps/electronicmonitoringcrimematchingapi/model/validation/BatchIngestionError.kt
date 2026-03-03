package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.BatchIngestionErrorType

data class BatchIngestionError(
  val errorType: BatchIngestionErrorType,
)
