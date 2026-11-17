package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import com.fasterxml.jackson.annotation.JsonInclude

// The Map Image Service rejects nulls for optional fields, so nulls must never be serialised
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MapImageServiceRequest(
  val viewport: Viewport,
  val layers: List<Layer>,
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Viewport(
    val mode: String = "fit",
  )

  sealed interface Layer {
    val type: String
    val id: String
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackLayer(
    override val id: String,
    val points: List<TrackPoint>,
  ) : Layer {
    override val type = "tracks"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class CirclesLayer(
    override val id: String,
    val points: List<CirclePoint>,
  ) : Layer {
    override val type = "circles"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class CirclePoint(
    val latitude: Double,
    val longitude: Double,
    val precision: Double,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class LocationsLayer(
    override val id: String,
    val points: List<Coordinate>,
  ) : Layer {
    override val type = "locations"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Coordinate(
    val latitude: Double,
    val longitude: Double,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TextLayer(
    override val id: String,
    val points: List<TextPoint>,
    val style: TextStyle,
  ) : Layer {
    override val type = "text"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TextStyle(
    val offset: TextOffset,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TextOffset(
    val y: Int,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TextPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String,
  )
}
