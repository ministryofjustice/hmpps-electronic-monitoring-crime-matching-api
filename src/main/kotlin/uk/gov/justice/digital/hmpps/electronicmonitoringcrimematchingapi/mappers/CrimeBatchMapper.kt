package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch

@Component
class CrimeBatchMapper(
  private val crimeMapper: CrimeMapper,
) {
  fun toDto(batch: CrimeBatch): CrimeBatchDto = CrimeBatchDto(
    id = batch.id.toString(),
    batchId = batch.batchId,
    crimes = batch.crimeVersions.map(crimeMapper::toDto),
  )
}
