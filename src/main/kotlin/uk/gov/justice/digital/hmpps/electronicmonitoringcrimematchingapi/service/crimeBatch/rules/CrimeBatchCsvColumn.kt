package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.rules

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.CsvConfig.CrimeBatchCsvConfig

enum class CrimeBatchCsvColumn(val index: Int) {
  POLICE_FORCE(CrimeBatchCsvConfig.ColumnsIndices.POLICE_FORCE),
  CRIME_TYPE_ID(CrimeBatchCsvConfig.ColumnsIndices.CRIME_TYPE_ID),
  BATCH_ID(CrimeBatchCsvConfig.ColumnsIndices.BATCH_ID),
  CRIME_REFERENCE(CrimeBatchCsvConfig.ColumnsIndices.CRIME_REFERENCE),
  CRIME_DATE_FROM(CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_FROM),
  CRIME_DATE_TO(CrimeBatchCsvConfig.ColumnsIndices.CRIME_DATE_TO),
  EASTING(CrimeBatchCsvConfig.ColumnsIndices.EASTING),
  NORTHING(CrimeBatchCsvConfig.ColumnsIndices.NORTHING),
  LATITUDE(CrimeBatchCsvConfig.ColumnsIndices.LATITUDE),
  LONGITUDE(CrimeBatchCsvConfig.ColumnsIndices.LONGITUDE),
  CRIME_TEXT(CrimeBatchCsvConfig.ColumnsIndices.CRIME_TEXT),
}
