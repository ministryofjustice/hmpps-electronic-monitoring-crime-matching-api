package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.UUID

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class SqsMessage(
  val Type: String,
  val Message: String,
  val MessageId: UUID,
)

class MessageBody(
  val receipt: Receipt,
)

class Receipt(
  val action: Action,
)

class Action(
  val objectKey: String,
  val bucketName: String,
)
