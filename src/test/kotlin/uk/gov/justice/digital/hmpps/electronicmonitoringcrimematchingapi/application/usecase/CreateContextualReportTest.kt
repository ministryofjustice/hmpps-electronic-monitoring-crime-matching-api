package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.usecase

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web.CreateContextualReportRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.ContextualReportCrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.HubManagerService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils.InMemoryCrimeMatchingResultDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils.InMemoryCrimeMatchingResultStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils.InMemoryPositionSegmentsProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils.PositionSegmentsRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class CreateContextualReportTest {
  private val clock = Clock.fixed(
    Instant.parse("2026-10-09T12:00:00Z"),
    ZoneOffset.UTC,
  )
  private val deviceWearerId = UUID.fromString("8b83e4b7-34a9-4ba7-8440-a7e5ed534cb4")
  private val hubManagerId = UUID.fromString("48b83e4b-ea09-4ba7-8440-a7e5ed534cb4")
  private val user = "user123"
  private val startTime = Instant.parse("2026-10-08T00:00:00Z")
  private val endTime = Instant.parse("2026-10-08T12:00:00Z")
  private val requestedAt = Instant.parse("2026-10-08T13:00:00Z")
  private val matchingResult = ContextualReportCrimeMatchingResult(
    batchId = "batch-1",
    crimeReference = "crime-1",
    crimeType = "burglary",
    matchingResultCreatedAt = Instant.parse("2026-10-07T10:00:00Z"),
    address = "1 Example Street",
    dateOfBirth = LocalDateTime.parse("1980-01-02T00:00:00"),
    identifier = "EMS-123",
    name = "Example Person",
    pncRef = "PNC-123",
    deviceId = 123L,
    crimeDateTimeFrom = Instant.parse("2026-10-08T00:00:00Z"),
    crimeDateTimeTo = Instant.parse("2026-10-08T12:00:00Z"),
  )
  private val hubManager = HubManager(name = "Example Manager", occupation = "Manager")
  private val positionSegments = listOf(
    listOf(
      Position(
        direction = 90,
        gpsDate = Instant.parse("2026-10-08T06:00:00Z"),
        latitude = 51.5074,
        longitude = -0.1278,
        precision = 5,
        speed = 10,
        sequenceNumber = 1,
      ),
    ),
  )
  private val request = CreateContextualReportRequest(
    startTime = startTime,
    endTime = endTime,
    policeForceArea = "Example Police",
    officerName = "Example Officer",
    hubManagerId = hubManagerId,
    requestedAt = requestedAt,
    additionalInformation = "Additional detail",
  )

  private val documentRequestStore = InMemoryDocumentRequestStore(clock)
  private val crimeMatchingResultStore = InMemoryCrimeMatchingResultStore(
    mapOf(deviceWearerId to matchingResult),
  )
  private val positionSegmentsProvider = InMemoryPositionSegmentsProvider(positionSegments)
  private val resultDocumentRequestStore = InMemoryCrimeMatchingResultDocumentRequestStore()
  private val hubManagerService = mock<HubManagerService>()
  private val createDocumentRequest = CreateDocumentRequest(
    store = documentRequestStore,
    mapper = DocumentRequestMapper(),
    parser = DocumentRequestParameterParser(
      JsonMapper.builder()
        .findAndAddModules()
        .build(),
    ),
    clock = clock,
  )
  private val useCase = CreateContextualReport(
    positionSegmentsProvider = positionSegmentsProvider,
    createDocumentRequest = createDocumentRequest,
    hubManagerService = hubManagerService,
    crimeMatchingResultStore = crimeMatchingResultStore,
    resultDocumentRequestStore = resultDocumentRequestStore,
  )

  @Test
  fun `it should create and persist a contextual report request and link it to the matched wearer`() {
    whenever(hubManagerService.getHubManager(hubManagerId)).thenReturn(hubManager)

    val documentRequestId = useCase.create(deviceWearerId, request, user)

    assertThat(resultDocumentRequestStore.saved).containsExactly(
      CrimeMatchingResultDocumentRequest(
        documentRequestId = UUID.fromString(documentRequestId),
        crimeMatchingResultDeviceWearerId = deviceWearerId,
      ),
    )
    assertThat(positionSegmentsProvider.lastRequest).isEqualTo(
      PositionSegmentsRequest(
        deviceId = "123",
        startTime = startTime.toString(),
        endTime = endTime.toString(),
      ),
    )
    verify(hubManagerService).getHubManager(hubManagerId)
  }

  @Test
  fun `it should not create a document request when the matched wearer does not exist`() {
    val unknownDeviceWearerId = UUID.randomUUID()

    assertThatThrownBy {
      useCase.create(unknownDeviceWearerId, request, user)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("No matched wearer with id $unknownDeviceWearerId")

    assertThat(resultDocumentRequestStore.saved).isEmpty()
    verifyNoInteractions(hubManagerService)
  }

  @Test
  fun `it should not save the link when the document request parameters are invalid`() {
    whenever(hubManagerService.getHubManager(hubManagerId)).thenReturn(hubManager)
    positionSegmentsProvider.positionSegments = emptyList()

    assertThatThrownBy {
      useCase.create(deviceWearerId, request, user)
    }
      .isInstanceOf(InvalidRequestException::class.java)
      .hasMessage("'positionSegments' must not be empty")

    assertThat(resultDocumentRequestStore.saved).isEmpty()
  }
}
