package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "document_request")
class DocumentRequestEntity(
  @Id
  val id: UUID,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val type: DocumentType,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val status: DocumentRequestStatus,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  val parameters: Map<String, Any>,

  @Column(name = "created_by", nullable = false)
  val createdBy: String,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,
)
