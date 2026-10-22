package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.InvalidRequestException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.ContextualReportDocxGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentSensitivityLabeller
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.processnextdocumentrequest.ProcessDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.config.DocumentProcessingConfiguration
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenRawContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentStore
import java.time.Clock
import java.time.Duration.ofSeconds
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class CreateDocumentRequestTest {
  private val store = InMemoryDocumentRequestStore()
  private val documentStore = InMemoryDocumentStore()

  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  private val processDocumentRequest = ProcessDocumentRequest(
    DocumentGeneratorRegistry(
      listOf(
        ContextualReportDocxGenerator(
          DocumentSensitivityLabeller(
            clock,
          ),
        ),
      ),
    ),
    documentStore = documentStore,
    documentRequestStore = store,
  )

  private val processNextDocumentRequest = spy(
    ProcessNextDocumentRequest(
      documentRequestStore = store,
      processDocumentRequest = processDocumentRequest,
      clock = clock,
    ),
  )

  private val objectMapper = JsonMapper.builder()
    .findAndAddModules()
    .build()

  private val useCase = CreateDocumentRequest(
    store = store,
    mapper = DocumentRequestMapper(),
    parser = DocumentRequestParameterParser(objectMapper),
    clock = clock,
    processNextDocumentRequest = processNextDocumentRequest,
  )

  @Test
  fun `it should create and persist a contextual report request`() {
    // Given a valid command
    val command = givenCreateDocumentRequestCommand()

    // When a document request is created
    val result = useCase.create(command, "user123")

    // Then the document request should be persisted
    val saved = store.findById(java.util.UUID.fromString(result.id))

    assertThat(saved).isNotNull
    assertThat(saved!!.id.toString()).isEqualTo(result.id)
    assertThat(saved.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(saved.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    assertThat(saved.createdBy).isEqualTo("user123")
    assertThat(saved.createdAt).isEqualTo(clock.instant())

    assertThat(saved.parameters).isEqualTo(
      givenContextualReportRequestParameters(),
    )
  }

  @Test
  fun `it should return while document processing is still running on another thread`() {
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    val processor = mock<ProcessDocumentRequest>()
    val requestStore = mock<DocumentRequestStore>()
    val savedRequest = AtomicReference<DocumentRequest>()

    whenever(requestStore.save(any())).thenAnswer {
      it.getArgument<DocumentRequest>(0).also(savedRequest::set)
    }
    whenever(requestStore.claimEligibleDocumentRequests(any(), any(), any())).thenAnswer {
      listOf(savedRequest.get())
    }
    doAnswer {
      started.countDown()
      release.await()
      null
    }.whenever(processor).process(any())

    AnnotationConfigApplicationContext().use { context ->
      context.register(DocumentProcessingConfiguration::class.java, ProcessNextDocumentRequest::class.java, CreateDocumentRequest::class.java)
      context.registerBean(DocumentRequestStore::class.java, Supplier { requestStore })
      context.registerBean(ProcessDocumentRequest::class.java, Supplier { processor })
      context.registerBean(Clock::class.java, Supplier { clock })
      context.registerBean(DocumentRequestMapper::class.java, Supplier { DocumentRequestMapper() })
      context.registerBean(DocumentRequestParameterParser::class.java, Supplier { DocumentRequestParameterParser(objectMapper) })
      context.refresh()

      try {
        val result = assertTimeoutPreemptively(ofSeconds(5)) {
          context.getBean(CreateDocumentRequest::class.java)
            .create(givenCreateDocumentRequestCommand(), "user123")
        }

        assertThat(started.await(5, SECONDS)).isTrue()
        assertThat(result).isEqualTo(DocumentRequestMapper().toSummary(savedRequest.get()))
        assertThat(result.status).isEqualTo(DocumentRequestStatus.PENDING)
      } finally {
        release.countDown()
      }
    }
  }

  @Test
  fun `it should return the persisted request when processing throws an exception`() {
    // Given that the document generation will fail
    doThrow(IllegalStateException("Document processing failed")).whenever(processNextDocumentRequest).process()

    // When a document request is created
    val result = useCase.create(givenCreateDocumentRequestCommand(), "user123")

    // Then the document request should be persisted
    val saved = store.findById(java.util.UUID.fromString(result.id))
    assertThat(saved).isNotNull
    assertThat(result).isEqualTo(DocumentRequestMapper().toSummary(saved!!))
    assertThat(result.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(result.createdBy).isEqualTo("user123")
    assertThat(result.createdAt).isEqualTo(clock.instant())
  }

  @Test
  fun `it should not persist a request with invalid parameters`() {
    // Given a command with invalid parameters
    val invalidCommand = givenCreateDocumentRequestCommand(
      parameters = givenRawContextualReportRequestParameters() + ("positionSegments" to emptyList<Any>()),
    )

    // When a document request is created, then it should throw an exception and not persist the request
    assertThatThrownBy {
      useCase.create(invalidCommand, "user123")
    }
      .isInstanceOf(InvalidRequestException::class.java)
      .hasMessage("'positionSegments' must not be empty")

    assertThat(store.findAll(pagination = PaginationRequest(page = 0, size = 20)).data).isEmpty()
  }

  @Test
  fun `it should create independent document requests`() {
    // Given a valid command
    val command = givenCreateDocumentRequestCommand()

    // When two document requests are created with the same command
    val first = useCase.create(command, "user123")
    val second = useCase.create(command, "user123")

    // Then the two requests should have been created
    assertThat(first.id).isNotEqualTo(second.id)
    assertThat(store.findAll(pagination = PaginationRequest(page = 0, size = 20)).data).hasSize(2)
  }

  private fun givenCreateDocumentRequestCommand(
    parameters: Map<String, Any> = givenRawContextualReportRequestParameters(),
  ): CreateDocumentRequestCommand = CreateDocumentRequestCommand(
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = parameters,
  )
}
