package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt

data class EmailIngestionPreparation(
  val crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt,
  val ingestionOutcome: EmailIngestionOutcome,
)
