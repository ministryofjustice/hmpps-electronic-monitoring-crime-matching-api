package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.GetDocumentRequestArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.ProcessNextDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.DocumentManagementApiExtension.Companion.documentManagementApi
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ProcessNextDocumentRequestIntegrationTest : IntegrationTestBase() {
  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  @Autowired
  private lateinit var useCase: ProcessNextDocumentRequest

  @Autowired
  private lateinit var documentRequestStore: DocumentRequestStore

  @Autowired
  private lateinit var repository: DocumentRequestRepository

  @Autowired
  private lateinit var getDocumentRequestArtefact: GetDocumentRequestArtefact

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  fun `it should claim a persisted pending request, complete it and store a retrievable artefact`() {
    // Given a pending document request persisted in the database
    val request = documentRequestStore.save(
      DocumentRequest.create(
        parameters = givenContextualReportRequestParameters(),
        createdBy = "user123",
        createdAt = Instant.parse("2026-10-08T12:00:00Z"),
      ),
    )

    // When the next document request is processed
    useCase.process()

    // Then the stored request should be completed with an artefact
    documentManagementApi.verifyUploadRequestCount(1)
    val persisted = requireNotNull(documentRequestStore.findById(request.id))
    assertThat(persisted.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    val artefact = requireNotNull(persisted.artefact)
    assertThat(artefact.filename).isEqualTo("placeholder")
    assertThat(artefact.contentType)
      .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document")

    // And the uploaded artefact should be retrievable via the stored reference
    documentManagementApi.stubGetDocumentFile(
      documentUuid = UUID.fromString(artefact.reference),
      responseBody = "uploaded contextual report",
      contentType = artefact.contentType,
    )
    val document = getDocumentRequestArtefact.get(request.id)
    assertThat(document.content.decodeToString()).isEqualTo("uploaded contextual report")
    assertThat(document.filename).isEqualTo(artefact.filename)
    assertThat(document.contentType).isEqualTo(artefact.contentType)
  }

  @Test
  fun `it should do nothing when there are no pending requests in the database`() {
    // Given no pending document requests in the database
    val request = documentRequestStore.save(
      DocumentRequest.rehydrate(
        id = UUID.randomUUID(),
        type = DocumentType.CONTEXTUAL_REPORT,
        status = DocumentRequestStatus.COMPLETED,
        parameters = givenContextualReportRequestParameters(),
        createdBy = "user123",
        createdAt = Instant.parse("2026-10-08T12:00:00Z"),
        claimedAt = null,
        artefact = null,
        attempts = 0,
        lastError = null,
        concurrencyVersion = 0L,
      ),
    )

    // When the next document request is processed
    useCase.process()

    // Then no document should be uploaded
    documentManagementApi.verifyUploadRequestCount(0)
    assertThat(repository.count()).isEqualTo(1)
    assertThat(repository.findById(request.id)).isPresent
  }

  @Test
  fun `it should not complete the request if the document management API rejects the upload`() {
    documentManagementApi.stubUploadDocumentFailure(400)
    val request = givenDocumentRequest()
    documentRequestStore.save(request)

    assertThrows<WebClientResponseException.BadRequest> {
      useCase.process()
    }

    val persisted = requireNotNull(documentRequestStore.findById(request.id))
    assertThat(persisted.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(persisted.artefact).isNull()
    documentManagementApi.verifyUploadRequestCount(1)
  }

  @Test
  fun `it should attempt to retry a 500 error from the document management API`() {
    documentManagementApi.stubUploadDocumentFailure(500)
    val request = givenDocumentRequest()
    documentRequestStore.save(request)

    val throwable = catchThrowable { useCase.process() }

    assertThat(throwable)
      .isInstanceOf(WebClientResponseException.InternalServerError::class.java)
    assertThat((throwable as WebClientResponseException.InternalServerError).statusCode.value()).isEqualTo(500)

    val persisted = requireNotNull(documentRequestStore.findById(request.id))
    assertThat(persisted.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(persisted.artefact).isNull()
    documentManagementApi.verifyUploadRequestCount(3)
  }

  private fun givenDocumentRequest(
    status: DocumentRequestStatus = DocumentRequestStatus.PENDING,
    claimedAt: Instant? = null,
    artefact: DocumentArtefact? = null,
  ): DocumentRequest = DocumentRequest.rehydrate(
    id = UUID.randomUUID(),
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = givenContextualReportRequestParameters(),
    createdBy = "user123",
    createdAt = clock.instant(),
    status = status,
    claimedAt = claimedAt,
    attempts = 0,
    lastError = null,
    concurrencyVersion = 0L,
    artefact = artefact,
  )
}
