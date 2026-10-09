package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.document.adapter.outbound.persistence

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import java.time.Instant
import java.util.UUID

class DocumentRequestPersistenceAdapterTest : IntegrationTestBase() {
  @Autowired
  private lateinit var store: DocumentRequestStore

  @Autowired
  private lateinit var repository: DocumentRequestRepository

  @Autowired
  private lateinit var entityManager: EntityManager

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Test
  @Transactional
  fun `it should rehydrate a document request with typed parameters`() {
    // Given a document request
    val request = DocumentRequest.create(
      parameters = ContextualReportRequestParameters(
        deviceId = 123456,
        from = Instant.parse("2026-10-01T08:30:15.123Z"),
        to = Instant.parse("2026-10-02T09:45:30.456Z"),
      ),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00.789Z"),
    )

    // When it is saved to the database
    store.save(request)

    // Flush and clear the persistence context to ensure we are retrieving from the database
    entityManager.flush()
    entityManager.clear()

    // And retrieved from the database
    val retrieved = store.findById(request.id)

    // Then it should be rehydrated into the domain model
    assertThat(retrieved).isNotNull

    assertThat(retrieved!!.id).isEqualTo(request.id)
    assertThat(retrieved.type).isEqualTo(request.type)
    assertThat(retrieved.parameters).isEqualTo(request.parameters)
    assertThat(retrieved.status).isEqualTo(request.status)
    assertThat(retrieved.createdBy).isEqualTo(request.createdBy)
    assertThat(retrieved.createdAt).isEqualTo(request.createdAt)
  }

  @Test
  fun `it should return null when a document request does not exist`() {
    val retrieved = store.findById(UUID.randomUUID())

    assertThat(retrieved).isNull()
  }
}
