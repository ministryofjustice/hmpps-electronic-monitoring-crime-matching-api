package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface CrimeVersionRepository : JpaRepository<CrimeVersion, UUID> {
  fun existsByCrimeTypeIdAndCrimeDateTimeFromAndCrimeDateTimeToAndEastingAndNorthingAndLatitudeAndLongitudeAndCrimeText(
    crimeTypeId: CrimeType,
    crimeDateTimeFrom: LocalDateTime,
    crimeDateTimeTo: LocalDateTime,
    easting: Double?,
    northing: Double?,
    latitude: Double?,
    longitude: Double?,
    crimeText: String,
  ): Boolean
}
