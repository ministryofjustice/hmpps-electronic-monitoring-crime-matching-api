package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.documentmanagement

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentUploadResponse(
  val documentUuid: UUID,
  val documentFilename: String,
  val mimeType: String,
)
