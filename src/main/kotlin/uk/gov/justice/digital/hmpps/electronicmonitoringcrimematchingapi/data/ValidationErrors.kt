package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data

class ValidationErrors {
  object Crime {
    const val INVALID_CRIME_REFERENCE: String = "A crime reference must be provided"
    const val INVALID_BATCH_ID: String = "A valid batch id must be provided"
  }
}
