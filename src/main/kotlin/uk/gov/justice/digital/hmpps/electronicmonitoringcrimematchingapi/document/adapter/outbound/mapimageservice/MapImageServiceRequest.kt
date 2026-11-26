package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import com.fasterxml.jackson.annotation.JsonInclude

// Optional fields must not be null, except circle fill, where null means outline only.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MapImageServiceRequest(
  val viewport: Viewport,
  val layers: List<Layer>,
  val controls: Controls? = null,
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Controls(
    val compass: Boolean,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Viewport(
    val mode: String = "fit",
    val padding: Int? = null,
  )

  sealed interface Layer {
    val type: String
    val id: String
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackLayer(
    override val id: String,
    val points: List<TrackPoint>,
    val style: TrackStyle? = null,
  ) : Layer {
    override val type = "tracks"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackStyle(
    val stroke: TrackStroke,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackStroke(
    val color: String,
    val lineDash: List<Int>? = null,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class CirclesLayer(
    override val id: String,
    val points: List<CirclePoint>,
    val style: CircleStyle,
  ) : Layer {
    override val type = "circles"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class CircleStyle(
    @get:JsonInclude(JsonInclude.Include.ALWAYS)
    val fill: String?,
    val stroke: CircleStroke,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class CircleStroke(
    val color: String,
    val lineDash: List<Int>,
    val width: Int,
  )

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
    val style: LocationStyle? = null,
  ) : Layer {
    override val type = "locations"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class LocationStyle(
    val fill: String,
    val stroke: LocationStroke,
  )

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class LocationStroke(
    val color: String,
  )

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
