package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.time.Instant

data class DocumentRequestSummary(
  val id: String,
  val type: DocumentType,
  val status: DocumentRequestStatus,
  val createdBy: String,
  val createdAt: Instant,
)
