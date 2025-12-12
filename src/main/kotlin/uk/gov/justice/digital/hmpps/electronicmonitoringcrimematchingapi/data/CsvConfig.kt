package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data

class CsvConfig {
  object CrimeBatchCsvConfig {
    const val COLUMN_COUNT = 13

    object ColumnsIndices {
      const val POLICE_FORCE = 0
      const val CRIME_TYPE_ID = 1
      const val BATCH_ID = 3
      const val CRIME_REFERENCE = 4
      const val CRIME_DATE_FROM = 5
      const val CRIME_DATE_TO = 6
      const val EASTING = 7
      const val NORTHING = 8
      const val LATITUDE = 9
      const val LONGITUDE = 10
      const val DATUM = 11
      const val CRIME_TEXT = 12
    }
  }
}
