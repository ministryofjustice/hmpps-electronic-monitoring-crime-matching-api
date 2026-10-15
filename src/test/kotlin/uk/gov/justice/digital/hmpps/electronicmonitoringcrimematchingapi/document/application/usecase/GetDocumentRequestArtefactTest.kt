package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.ArtefactFailedToGenerateException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestArtefactNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentStore
import java.time.Instant
import java.util.UUID

class GetDocumentRequestArtefactTest {
  private val documentRequestStore = InMemoryDocumentRequestStore()
  private val documentStore = InMemoryDocumentStore()
  private val useCase = GetDocumentRequestArtefact(documentRequestStore, documentStore)

  @Test
  fun `it should get the generated document for an artefact if it exists`() {
    // Given a saved document request with a generated document artefact
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )
    val generatedDocument = GeneratedDocument(
      filename = "contextual-report.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = byteArrayOf(1, 2, 3),
    )
    val artefact = documentStore.save(generatedDocument)
    request.complete(artefact)
    documentRequestStore.save(request)

    // When the generated document is retrieved
    val result = useCase.get(request.id)

    // Then the document is returned
    assertThat(result.filename).isEqualTo(generatedDocument.filename)
    assertThat(result.contentType).isEqualTo(generatedDocument.contentType)
    assertThat(result.content).isEqualTo(generatedDocument.content)
  }

  @Test
  fun `it should throw DocumentRequestNotFoundException if the document request does not exist`() {
    // Given a non-existent document request ID
    val id = UUID.randomUUID()

    // When the generated document is retrieved, then an error is raised
    assertThatThrownBy {
      useCase.get(id)
    }
      .isInstanceOf(DocumentRequestNotFoundException::class.java)
      .hasMessage("Document request with id $id not found")
  }

  @Test
  fun `it should throw DocumentRequestArtefactNotFoundException if the artefact does not exist`() {
    // Given a saved document request without an artefact
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )
    documentRequestStore.save(request)

    // When the generated document is retrieved, then an error is raised
    assertThatThrownBy {
      useCase.get(request.id)
    }
      .isInstanceOf(DocumentRequestArtefactNotFoundException::class.java)
      .hasMessage("Document artefact for request with id ${request.id} not found")
  }

  @Test
  fun `it should throw DocumentRequestArtefactNotFoundException if the artefact does not have a related generated document`() {
    // Given a saved document request with an artefact that has no generated document in storage
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )
    val artefact = DocumentArtefact(
      reference = "missing-document-reference",
      filename = "missing.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )
    request.complete(artefact)
    documentRequestStore.save(request)

    // When the generated document is retrieved, then an error is raised
    assertThatThrownBy {
      useCase.get(request.id)
    }
      .isInstanceOf(DocumentRequestArtefactNotFoundException::class.java)
      .hasMessage("Document artefact for request with id ${request.id} not found")
  }

  @Test
  fun `it should throw ArtefactFailedToGenerateException if the document request is dead`() {
    // Given a saved document request with a dead status
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )
    request.failPermanently()
    documentRequestStore.save(request)

    // When the generated document is retrieved, then an error is raised
    assertThatThrownBy {
      useCase.get(request.id)
    }
      .isInstanceOf(ArtefactFailedToGenerateException::class.java)
      .hasMessage("Document artefact for request with id ${request.id} failed to generate")
  }
}
