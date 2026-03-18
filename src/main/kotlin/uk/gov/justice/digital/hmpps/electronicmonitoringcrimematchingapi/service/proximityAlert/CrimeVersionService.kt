package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerPositionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceWearerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection
import java.util.UUID

@Service
class CrimeVersionService(
  private val crimeVersionRepository: CrimeVersionRepository,
) {

  fun searchCrimeVersions(
    crimeRef: String,
    page: Int,
    pageSize: Int,
  ): Page<CrimeVersionSummaryProjection> = crimeVersionRepository.findCrimeVersionsByCrimeReference(
    crimeReference = crimeRef,
    pageable = PageRequest.of(page, pageSize),
  )

  fun getCrimeVersion(id: UUID): CrimeVersionResponse {
    val results = crimeVersionRepository.findCrimeVersionMatchingResult(id)

    if (results.isEmpty()) {
      throw EntityNotFoundException("No crime version found with id: $id")
    }

    val crimeVersion = results.first()

    val deviceWearerMap = LinkedHashMap<String, DeviceWearerResponse>()
    results.forEach { row ->
      if (row.deviceWearerId != null) {
        val wearer = deviceWearerMap.getOrPut(row.deviceWearerId!!) {
          DeviceWearerResponse(
            name = row.name!!,
            deviceId = row.deviceId!!,
            nomisId = row.nomisId!!,
          )
        }

        wearer.positions += DeviceWearerPositionResponse(
          latitude = row.latitude!!,
          longitude = row.longitude!!,
          sequenceLabel = row.sequenceLabel!!,
          confidence = row.confidence!!,
          captureDateTime = row.capturedDateTime.toString(),
        )
      }
    }

    val crimeVersionResponse = CrimeVersionResponse(
      crimeReference = crimeVersion.crimeReference,
      crimeType = crimeVersion.crimeType.value,
      crimeDateTimeFrom = crimeVersion.crimeDateTimeFrom.toString(),
      crimeDateTimeTo = crimeVersion.crimeDateTimeTo.toString(),
      crimeText = crimeVersion.crimeText,
      deviceWearers = deviceWearerMap.values.toList(),
    )

    return crimeVersionResponse
  }
}
