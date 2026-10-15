package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.LocalDate

data class DeviceWearerDetails(
  val address: String,
  val dateOfBirth: LocalDate,
  val emsId: String,
  val pncNumber: String,
  val name: String,
  val offenderManager: String,
) {
  init {
    require(address.isNotBlank()) {
      "'address' cannot be blank"
    }

    require(emsId.isNotBlank()) {
      "'emsId' cannot be blank"
    }

    require(name.isNotBlank()) {
      "'name' cannot be blank"
    }

    require(offenderManager.isNotBlank()) {
      "'offenderManager' cannot be blank"
    }
  }
}
