package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

data class FailedRecord(
    val rowNumber: Int,
    val errorMessage: String,
    val originalCsvRow: String? = null,
)