package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.outbox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.NamedDataSource
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

@ActiveProfiles("test")
class EmailOutboxPayloadMapperTest {
  private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
  private val mapper = EmailOutboxPayloadMapper(objectMapper)

  private fun outcome() = EmailIngestionOutcome(
    batchId = "batchId",
    crimeBatchId = "142a9a57-337f-4208-908b-2874b75fa10d",
    policeForce = "Metropolitan",
    emailData = EmailData(
      sender = "sender@police.gov.uk",
      originalSender = "officer@police.gov.uk",
      subject = "subject",
      sentAt = Date(),
      attachments = listOf(NamedDataSource("crimes.csv")),
    ),
    records = listOf(
      CrimeRecordRequest(
        policeForce = PoliceForce.METROPOLITAN,
        crimeTypeId = CrimeType.AB,
        batchId = "batchId",
        crimeReference = "CRI00000001",
        crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30).toInstant(ZoneOffset.UTC),
        crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 9, 30).toInstant(ZoneOffset.UTC),
        easting = null,
        northing = null,
        latitude = 54.73241,
        longitude = -1.38542,
        crimeText = "text",
      ),
    ),
    recordCount = 1,
    ingestionStatus = IngestionStatus.SUCCESSFUL,
  )

  @Test
  fun `it should round-trip an ingestion outcome through the payload`() {
    val json = mapper.toJson(outcome(), "officer@police.gov.uk")

    val payload = mapper.readPayload(json)
    val rebuilt = mapper.toOutcome(payload)

    assertThat(payload.recipient).isEqualTo("officer@police.gov.uk")
    assertThat(rebuilt.ingestionStatus).isEqualTo(IngestionStatus.SUCCESSFUL)
    assertThat(rebuilt.batchId).isEqualTo("batchId")
    assertThat(rebuilt.crimeBatchId).isEqualTo("142a9a57-337f-4208-908b-2874b75fa10d")
    assertThat(rebuilt.policeForce).isEqualTo("Metropolitan")
    assertThat(rebuilt.emailData.attachments.first().name).isEqualTo("crimes.csv")
    assertThat(rebuilt.records).hasSize(1)
    assertThat(rebuilt.records.first().crimeReference).isEqualTo("CRI00000001")
    assertThat(rebuilt.records.first().crimeDateTimeFrom)
      .isEqualTo(LocalDateTime.of(2025, 1, 25, 8, 30).toInstant(ZoneOffset.UTC))
  }

  @Test
  fun `it should default the file name when there is no attachment`() {
    val withoutAttachment = outcome().copy(
      emailData = EmailData(
        sender = "sender@police.gov.uk",
        originalSender = "officer@police.gov.uk",
        subject = "subject",
        sentAt = Date(),
        attachments = emptyList(),
      ),
    )

    val rebuilt = mapper.toOutcome(mapper.readPayload(mapper.toJson(withoutAttachment, "sender@police.gov.uk")))

    assertThat(rebuilt.emailData.attachments.first().name).isEqualTo("Invalid File")
  }
}
