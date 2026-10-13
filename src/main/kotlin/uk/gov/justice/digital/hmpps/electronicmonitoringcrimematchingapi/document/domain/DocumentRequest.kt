package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import java.time.Instant
import java.util.UUID

class DocumentRequest private constructor(
  val id: UUID,
  val type: DocumentType,
  val parameters: DocumentRequestParameters,
  status: DocumentRequestStatus,
  val createdBy: String,
  val createdAt: Instant,
  artefact: DocumentArtefact? = null,
) {
  var status: DocumentRequestStatus = status
    private set

  var artefact = artefact
    private set

  init {
    require(type == parameters.type) {
      "Document type $type does not match parameters type ${parameters.type}"
    }
  }

  fun startProcessing() {
    check(status == DocumentRequestStatus.PENDING) {
      "Only pending document requests can be processed"
    }
    status = DocumentRequestStatus.IN_PROGRESS
  }

  fun complete(artefact: DocumentArtefact) {
    check(status == DocumentRequestStatus.IN_PROGRESS) {
      "Only in-progress document requests can be completed"
    }
    this.artefact = artefact
    status = DocumentRequestStatus.COMPLETED
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
    )

    fun rehydrate(
      id: UUID,
      type: DocumentType,
      parameters: DocumentRequestParameters,
      status: DocumentRequestStatus,
      createdBy: String,
      createdAt: Instant,
    ): DocumentRequest = DocumentRequest(
      id = id,
      type = type,
      parameters = parameters,
      status = status,
      createdBy = createdBy,
      createdAt = createdAt,
    )
  }
}
