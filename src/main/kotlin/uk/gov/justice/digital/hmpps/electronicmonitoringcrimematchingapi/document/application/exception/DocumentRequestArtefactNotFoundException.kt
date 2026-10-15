package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception

class DocumentRequestArtefactNotFoundException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
