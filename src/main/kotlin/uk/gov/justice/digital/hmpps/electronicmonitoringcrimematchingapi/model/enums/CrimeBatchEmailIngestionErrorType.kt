package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailIngestionErrorType(val message: String) {
  MULTIPLE_POLICE_FORCES("Multiple police forces found in csv file"),
  MULTIPLE_BATCH_IDS("Multiple batch Ids found in csv file"),
  INVALID_ATTACHMENT("One csv attachment expected"),
  INVALID_CSV_FORMAT("The CSV file attache,ent could not be read. The file may be corrupt or in an unexpected format."),
  ALL_RECORDS_FAILED("All records in the CSV file failed validation. No data has been ingested."),
  UNEXPECTED_ERROR("An unexpected error occurred during ingestion. Please contact EM AC hun for support if this persists."),
}
