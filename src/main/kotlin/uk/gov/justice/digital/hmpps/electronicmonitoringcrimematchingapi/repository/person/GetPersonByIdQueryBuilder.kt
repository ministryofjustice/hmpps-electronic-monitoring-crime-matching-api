package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Person

class GetPersonByIdQueryBuilder(private val id: String) {
  fun build(): AthenaQuery = Person
    .select(
      Person.deviceWearerId,
      Person.firstName,
      Person.lastName,
      Person.nomisId,
      Person.dateOfBirth,
      Person.postcode,
      Person.cityOrTown,
      Person.street,
    )
    .where {
      Person.deviceWearerId eq id
    }
    .prepare()
}
