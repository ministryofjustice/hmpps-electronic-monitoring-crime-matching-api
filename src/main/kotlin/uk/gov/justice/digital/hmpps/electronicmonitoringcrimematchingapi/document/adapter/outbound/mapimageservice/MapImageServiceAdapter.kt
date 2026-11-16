package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRenderer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest

@Component
class MapImageServiceAdapter(
  private val client: MapImageServiceClient,
) : MapImageRenderer {
  override fun render(request: MapImageRequest): ByteArray = client.render(
    MapImageServiceRequest(
      viewport = MapImageServiceRequest.Viewport(
        centre = MapImageServiceRequest.Coordinate(
          latitude = request.centreLatitude,
          longitude = request.centreLongitude,
        ),
        zoom = request.zoom,
      ),
    ),
  )
}
