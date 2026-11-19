package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRenderer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest

@Component
class MapImageServiceAdapter(
  private val client: MapImageServiceClient,
) : MapImageRenderer {
  override fun render(request: MapImageRequest): ByteArray {
    val positions = request.positions

    val layers = buildList {
      if (positions.size > 1) {
        add(
          MapImageServiceRequest.TrackLayer(
            id = "position-track",
            points = positions.map { position ->
              MapImageServiceRequest.TrackPoint(
                latitude = position.latitude,
                longitude = position.longitude,
              )
            },
          ),
        )
      }

      add(
        MapImageServiceRequest.CirclesLayer(
          id = "position-confidence-circles",
          points = positions.map { position ->
            MapImageServiceRequest.CirclePoint(
              latitude = position.latitude,
              longitude = position.longitude,
              precision = position.precision.toDouble(),
            )
          },
        ),
      )

      add(
        MapImageServiceRequest.LocationsLayer(
          id = "position-locations",
          points = positions.map { position ->
            MapImageServiceRequest.Coordinate(
              latitude = position.latitude,
              longitude = position.longitude,
            )
          },
        ),
      )

      add(
        MapImageServiceRequest.TextLayer(
          id = "position-sequence-numbers",
          points = positions.map { position ->
            MapImageServiceRequest.TextPoint(
              latitude = position.latitude,
              longitude = position.longitude,
              label = position.sequenceNumber.toString(),
            )
          },
          style = MapImageServiceRequest.TextStyle(
            offset = MapImageServiceRequest.TextOffset(y = 18),
          ),
        ),
      )
    }

    return client.render(
      MapImageServiceRequest(
        viewport = MapImageServiceRequest.Viewport(),
        layers = layers,
      ),
    )
  }
}
