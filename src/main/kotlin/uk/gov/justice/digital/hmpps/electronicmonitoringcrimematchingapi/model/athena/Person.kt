package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table

object Person : Table(name = "caseload") {
  val personId = long("mdss_person_id")
  val firstName = varchar("first_name")
  val lastName = varchar("last_name")
  val nomisId = varchar("nomis_id")
  val dateOfBirth = date("date_of_birth")
  val postcode = varchar("postcode")
  val cityOrTown = varchar("city_or_town")
  val street = varchar("house_number_and_street_name")
}
