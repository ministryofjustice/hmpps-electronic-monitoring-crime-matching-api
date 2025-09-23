package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.SimpleResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.AthenaRepository
import java.util.Optional

@Repository
class DeviceActivationRepository(
  athenaClient: EmDatastoreClient,
) : AthenaRepository<DeviceActivation>(athenaClient) {

  override val resultSetExtractor = SimpleResultSetExtractor(DeviceActivation::class.java)

  fun findById(id: Long): Optional<DeviceActivation> = Optional.ofNullable(
    this.executeQuery(
      GetDeviceActivationByIdQueryBuilder(
        athenaClient.properties,
        id,
      ).build(),
    ).firstOrNull(),
  )
}
