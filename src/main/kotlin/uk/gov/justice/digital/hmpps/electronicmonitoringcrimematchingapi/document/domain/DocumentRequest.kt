package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import java.time.Instant
import java.util.UUID

class DocumentRequest private constructor(
  val id: UUID,
  val type: DocumentType,
  val parameters: DocumentRequestParameters,
  var status: DocumentRequestStatus,
  val createdBy: String,
  val createdAt: Instant,
  val claimedAt: Instant?,
  val attempts: Int,
  val lastError: String?,
  val concurrencyVersion: Long,
) {

  init {
    require(type == parameters.type) {
      "Document type $type does not match parameters type ${parameters.type}"
    }
  }

  companion object {
    fun create(
      parameters: DocumentRequestParameters,
      createdBy: String,
      createdAt: Instant,
    ): DocumentRequest = DocumentRequest(
      id = UUID.randomUUID(),
      type = parameters.type,
      parameters = parameters,
      status = DocumentRequestStatus.PENDING,
      createdBy = createdBy,
      createdAt = createdAt,
      claimedAt = null,
      attempts = 0,
      lastError = null,
      concurrencyVersion = 0,
    )

    fun rehydrate(
      id: UUID,
      type: DocumentType,
      parameters: DocumentRequestParameters,
      status: DocumentRequestStatus,
      createdBy: String,
      createdAt: Instant,
      claimedAt: Instant?,
      attempts: Int,
      lastError: String?,
      concurrencyVersion: Long,
    ): DocumentRequest = DocumentRequest(
      id = id,
      type = type,
      parameters = parameters,
      status = status,
      createdBy = createdBy,
      createdAt = createdAt,
      claimedAt = claimedAt,
      attempts = attempts,
      lastError = lastError,
      concurrencyVersion = concurrencyVersion,
    )
  }
}
