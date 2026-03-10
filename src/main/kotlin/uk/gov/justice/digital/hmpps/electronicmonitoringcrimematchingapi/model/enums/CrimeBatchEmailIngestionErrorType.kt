package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeBatchEmailIngestionErrorType(val message: String) {
  MULTIPLE_POLICE_FORCES("Multiple police forces found in csv file"),
  MULTIPLE_BATCH_IDS("Multiple batch Ids found in csv file"),
  DUPLICATE_BATCH_ID("Batch ID already exists in the system"),
  INVALID_ATTACHMENT("One csv attachment expected"),
}
