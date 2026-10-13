package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

data class DocumentArtefact(
  val reference: String,
  val filename: String,
  val contentType: String,
)
