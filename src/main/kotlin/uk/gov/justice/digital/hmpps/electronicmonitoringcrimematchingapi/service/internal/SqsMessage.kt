package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SqsMessage(
  val Type: String,
  val Message: String,
  val MessageId: UUID,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class MessageBody(
  val receipt: Receipt,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class Receipt(
  val action: Action,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class Action(
  val objectKey: String,
  val bucketName: String,
)