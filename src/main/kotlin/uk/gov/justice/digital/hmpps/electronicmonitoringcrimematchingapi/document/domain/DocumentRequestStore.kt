package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import java.util.UUID

interface DocumentRequestStore {
  fun save(
    documentRequest: DocumentRequest,
  ): DocumentRequest

  fun findById(id: UUID): DocumentRequest?
}
