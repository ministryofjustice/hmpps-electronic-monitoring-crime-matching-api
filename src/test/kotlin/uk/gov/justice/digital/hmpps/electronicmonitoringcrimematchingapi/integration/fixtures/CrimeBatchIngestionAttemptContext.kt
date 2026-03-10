package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment

class CrimeBatchIngestionAttemptContext(
  private val crimeBatchEmail: CrimeBatchEmail,
) {
  fun withAttachment(
    rowCount: Int = 1,
    block: CrimeBatchEmailAttachmentContext.() -> Unit = {},
  ) {
    val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
      crimeBatchEmail = crimeBatchEmail,
      fileName = "test.csv",
      rowCount = rowCount,
    )

    CrimeBatchEmailAttachmentContext(crimeBatchEmailAttachment).block()

    crimeBatchEmail.crimeBatchEmailAttachments.add(
      crimeBatchEmailAttachment,
    )
  }
}
