package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultSummaryProjection

class CrimeMatchingResultSummaryMapper {
  fun toDto(projection: CrimeMatchingResultSummaryProjection): CrimeMatchingResultSummaryResponse = CrimeMatchingResultSummaryResponse(
    policeForceArea = projection.policeForceArea.label,
    batchId = projection.batchId,
    automatedMatches = projection.automatedMatches,
  )
}
