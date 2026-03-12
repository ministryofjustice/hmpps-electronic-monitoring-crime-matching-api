package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection

@Component
class CrimeVersionSummaryMapper {

  fun toDto(summary: CrimeVersionSummaryProjection): CrimeVersionSummaryResponse = CrimeVersionSummaryResponse(
    crimeVersionId = summary.crimeVersionId,
    crimeReference = summary.crimeReference,
    policeForceArea = summary.policeForceArea,
    crimeType = summary.crimeTypeId,
    crimeDate = summary.crimeDateTimeFrom.toLocalDate().toString(),
    batchId = summary.batchId,
    ingestionDateTime = summary.ingestionDateTime.toString(),
    matched = summary.matched,
    versionLabel = summary.versionLabel,
    updates = summary.updates,
  )
}
