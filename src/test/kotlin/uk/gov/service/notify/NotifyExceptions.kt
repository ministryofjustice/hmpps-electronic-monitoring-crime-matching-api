package uk.gov.service.notify

/**
 * Test-only factory that reaches the package-private
 * [NotificationClientException] constructor carrying an HTTP status code.
 */
object NotifyExceptions {
  fun withStatus(status: Int, message: String): NotificationClientException = NotificationClientException(status, message)
}
