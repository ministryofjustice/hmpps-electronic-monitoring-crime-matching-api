package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.cast
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.CurrentDate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Person
import java.time.ZonedDateTime

class GetPersonByIdQueryBuilder(private val id: String) {
  fun build(): AthenaQuery = Person
    .select(
      Person.deviceWearerId,
      Person.firstName,
      Person.lastName,
      Person.nomisId,
      Person.pncId,
      Person.dateOfBirth,
      Person.responsibleOfficerName,
      Person.postcode,
      Person.cityOrTown,
      Person.street,
    )
    .where {
      Person.deviceWearerId eq id

      Person.groupedDate.cast<ZonedDateTime>(SqlType.Date) eq CurrentDate()
    }
    .prepare()
}
