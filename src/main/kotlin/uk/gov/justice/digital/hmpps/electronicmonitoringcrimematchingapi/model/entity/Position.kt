package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class Position(
  val positionId: Long,

  val positionLatitude: Double,

  val positionLongitude: Double,

  val positionPrecision: Long,

  val positionSpeed: Long,

  val positionDirection: Long,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[.[SSSSSS][SSS]]")
  val positionGpsDate: LocalDateTime,

  val positionLbs: Long,
)
