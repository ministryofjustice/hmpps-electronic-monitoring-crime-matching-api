package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

interface DocumentStore {
  fun save(document: GeneratedDocument): DocumentArtefact

  fun get(documentArtefact: DocumentArtefact): GeneratedDocument?
}
