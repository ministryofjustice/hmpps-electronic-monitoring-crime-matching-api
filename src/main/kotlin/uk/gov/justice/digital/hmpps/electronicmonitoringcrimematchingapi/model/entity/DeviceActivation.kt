package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class DeviceActivation(
  val deviceActivationId: Long,

  val deviceId: Long,

  val deviceName: String = "",

  val personId: Long,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[.[SSSSSS][SSS]]")
  val deviceActivationDate: LocalDateTime,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss[.[SSSSSS][SSS]]")
  val deviceDeactivationDate: LocalDateTime?,

  val orderStart: String = "",

  val orderEnd: String = "",
)
