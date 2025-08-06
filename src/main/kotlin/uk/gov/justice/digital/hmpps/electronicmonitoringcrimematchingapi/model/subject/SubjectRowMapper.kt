package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class SubjectRowMapper : RowMapper<Subject> {
  override fun mapRow(row: ResultSet, rowNumber: Int): Subject {
    return Subject(
      row.getString("person_id"),
      nomisId = row.getString("nomis_id"),
      name = row.getString("name"),
      dateOfBirth = row.getString("date_of_birth"),
      address = row.getString("address"),
      orderStartDate = null,
      orderEndDate = null,
      deviceId = row.getString("device_id"),
      tagPeriodStartDate = null,
      tagPeriodEndDate = null,
    )
  }
}