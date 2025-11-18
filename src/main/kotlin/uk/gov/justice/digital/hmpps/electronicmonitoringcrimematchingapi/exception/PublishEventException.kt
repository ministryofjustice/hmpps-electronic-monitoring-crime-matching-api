package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.exception

import java.util.function.Supplier

class PublishEventException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PublishEventException> {
  override fun get(): PublishEventException = PublishEventException(message, cause)
}
