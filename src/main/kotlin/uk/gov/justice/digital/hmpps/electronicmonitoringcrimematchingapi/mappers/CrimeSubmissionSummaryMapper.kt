package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeSubmissionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeSubmissionSummaryProjection

class CrimeSubmissionSummaryMapper {
  fun toDto(
    crimeType: CrimeType?,
    projection: CrimeSubmissionSummaryProjection?,
  ): CrimeSubmissionSummaryResponse = CrimeSubmissionSummaryResponse(
    crimeType = crimeType?.value ?: "Missing",
    submitted = projection?.submitted ?: 0,
    ingested = projection?.ingested ?: 0,
    failed = projection?.failed ?: 0,
    notIngestedPercentage = projection?.notIngestedPercentage ?: 0.0,
  )
}
