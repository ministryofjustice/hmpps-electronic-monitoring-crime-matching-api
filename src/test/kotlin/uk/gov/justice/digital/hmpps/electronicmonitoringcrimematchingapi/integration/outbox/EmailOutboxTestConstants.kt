package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.outbox

/**
 * Test constants and configuration for email outbox integration tests.
 */
object EmailOutboxTestConstants {

  /**
   * Standard test outbox lease timeout (in milliseconds).
   * Keep low to avoid long waits in tests (actual production is 120000ms).
   */
  const val TEST_LEASE_TIMEOUT_MS = 5000L

  /**
   * Standard test batch size for relay claiming.
   */
  const val TEST_BATCH_SIZE = 50

  /**
   * Standard test max receive count before DLQ.
   */
  const val TEST_MAX_RECEIVE_COUNT = 3

  /**
   * Timeout for Awaitility awaits in integration tests (in milliseconds).
   */
  const val AWAIT_TIMEOUT_MS = 5000L

  /**
   * Poll interval for Awaitility conditions (in milliseconds).
   */
  const val AWAIT_POLL_INTERVAL_MS = 100L

  /**
   * S3 bucket name for test email uploads.
   */
  const val TEST_BUCKET_NAME = "test-email-bucket"

  /**
   * S3 object key for test email file.
   */
  const val TEST_OBJECT_KEY = "test-email.eml"

  /**
   * Default test sender email address.
   */
  const val TEST_SENDER = "shared-mailbox@email.com"

  /**
   * Default test original sender (police officer) email.
   */
  const val TEST_ORIGINAL_SENDER = "officer@police.gov.uk"

  /**
   * Default test subject line.
   */
  const val TEST_SUBJECT = "Metropolitan - Crime Mapping Request - 20250815"

  /**
   * Default test police force name.
   */
  const val TEST_POLICE_FORCE = "Metropolitan"
}
