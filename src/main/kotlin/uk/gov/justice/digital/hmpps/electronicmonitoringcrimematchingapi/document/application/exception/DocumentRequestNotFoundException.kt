package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception

class DocumentRequestNotFoundException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
