package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Caseload

class GetPersonByIdQueryBuilder(private val id: Long) {
  fun build(): AthenaQuery = Caseload
    .select(
      Caseload.personId,
      Caseload.firstName,
      Caseload.lastName,
      Caseload.nomisId,
      Caseload.dateOfBirth,
      Caseload.postcode,
      Caseload.cityOrTown,
      Caseload.street,
    )
    .where {
      Caseload.personId eq id
    }
    .prepare()
}
