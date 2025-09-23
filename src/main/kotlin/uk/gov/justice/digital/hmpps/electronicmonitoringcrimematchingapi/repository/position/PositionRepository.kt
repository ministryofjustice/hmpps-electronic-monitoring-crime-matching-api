package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.SimpleResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.AthenaRepository
import java.time.ZonedDateTime

@Repository
class PositionRepository(
  athenaClient: EmDatastoreClient,
) : AthenaRepository<Position>(athenaClient) {

  override val resultSetExtractor = SimpleResultSetExtractor(Position::class.java)

  fun findByDeviceActivationId(
    id: Long,
    geolocationMechanism: GeolocationMechanism?,
    from: ZonedDateTime?,
    to: ZonedDateTime?,
  ): List<Position> = this.executeQuery(
    GetPositionsByDeviceActivationId(
      athenaClient.properties,
      id,
      geolocationMechanism,
      from,
      to,
    ).build(),
  )
}
