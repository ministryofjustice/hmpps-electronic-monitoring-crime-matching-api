package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRenderer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest

private const val POSITION_COLOR = "#f2a93b" // Dark yellow

@Component
class MapImageServiceAdapter(
  private val client: MapImageServiceClient,
) : MapImageRenderer {
  override fun render(request: MapImageRequest): ByteArray {
    val positions = request.positions

    val layers = buildList {
      if (request.includeTrack) {
        addAll(positionTrackLayers(positions))
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
          style = MapImageServiceRequest.CircleStyle(
            fill = null,
            stroke = MapImageServiceRequest.CircleStroke(
              color = POSITION_COLOR,
              lineDash = listOf(8, 3),
              width = 2,
            ),
          ),
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
          style = MapImageServiceRequest.LocationStyle(
            fill = POSITION_COLOR,
            stroke = MapImageServiceRequest.LocationStroke(color = "#00000000"),
          ),
        ),
      )

      if (request.includeSequenceNumbers) {
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
    }

    return client.render(
      MapImageServiceRequest(
        viewport = MapImageServiceRequest.Viewport(padding = request.viewportPaddingPx),
        layers = layers,
        controls = MapImageServiceRequest.Controls(compass = true),
      ),
    )
  }
}
