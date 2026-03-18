package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.IngestionAttemptCrimesByTypeResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.IngestionAttemptCrimesByTypeProjection

@Component
class IngestionAttemptCrimesByTypeMapper {
  fun toDto(crimeSummary: IngestionAttemptCrimesByTypeProjection): IngestionAttemptCrimesByTypeResponse = IngestionAttemptCrimesByTypeResponse(
    crimeType = crimeSummary.crimeType ?: "MISSING",
    submitted = crimeSummary.submitted,
    failed = crimeSummary.failed,
    successful = crimeSummary.successful,
  )
}
