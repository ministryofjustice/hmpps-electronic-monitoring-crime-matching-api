package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data

class ValidationErrors {
  object Crime {
    const val INVALID_CRIME_REFERENCE: String = "A crime reference must be provided"
    const val INVALID_CRIME_DATE: String = "A valid crime date range must be provided"
  }
}
