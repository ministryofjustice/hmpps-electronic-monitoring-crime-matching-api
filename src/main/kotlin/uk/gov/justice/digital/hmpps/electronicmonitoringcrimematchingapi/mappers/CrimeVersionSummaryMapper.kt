package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion

@Component
class CrimeVersionSummaryMapper {

  fun toDto(crimeVersion: CrimeVersion): CrimeVersionSummaryResponse = CrimeVersionSummaryResponse(
    crimeVersionId = crimeVersion.id.toString(),
    crimeReference = crimeVersion.crime.crimeReference,
    policeForceArea = crimeVersion.crime.policeForceArea.name,
    crimeType = crimeVersion.crimeTypeId.name,
    crimeDate = crimeVersion.crimeDateTimeFrom.toString(),
    batchId = crimeVersion.crimeBatch.batchId,
    ingestionDateTime = crimeVersion.crimeBatch.createdAt.toString(),
    matched = crimeVersion.matchingResults.isNotEmpty(),
    versionLabel = crimeVersion.versionLabel,
    updates = crimeVersion.updatesSummary,
  )
}
