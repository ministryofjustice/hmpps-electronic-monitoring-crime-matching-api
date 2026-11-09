package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import java.util.UUID

fun HubManagerRepository.createHubManager(
  id: String,
  name: String,
  occupation: String,
  hasSignature: Boolean,
): HubManager {
  val manager = HubManager(id = UUID.fromString(id), name = name, occupation = occupation)

  if (hasSignature) {
    manager.signatureImage = "fake-signature-image".toByteArray()
    manager.signatureImageContentType = "image/png"
  }

  return save(manager)
}
