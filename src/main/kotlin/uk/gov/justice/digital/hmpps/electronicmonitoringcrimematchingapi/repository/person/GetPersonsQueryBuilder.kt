package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilderUpdate

class GetPersonsQueryBuilder(
  datastoreProperties: DatastoreProperties,
  criteria: PersonsQueryCriteria,
) : SqlQueryBuilderUpdate("${datastoreProperties.database}.caseload", "c") {

  init {
    val includeDevices = criteria.includeDeviceActivations
    val db = datastoreProperties.database

    select(
      "c.mdss_person_id",
      "c.first_name",
      "c.last_name",
      "c.nomis_id",
      "c.date_of_birth",
      "c.postcode",
      "c.city_or_town",
      "c.house_number_and_street_name",
      *if (includeDevices) arrayOf(
        "da.device_id",
        "da.device_activation_id",
        "da.device_activation_date",
        "da.device_deactivation_date",
      ) else emptyArray()
    )

    if (includeDevices) {
      join(
        "$db.device_activations da",
        "da.person_id = c.mdss_person_id",
        JoinType.INNER
      )
    }

    where {
      if (!criteria.name.isNullOrBlank()) {
        or {
          "c.first_name" like criteria.name
          "c.last_name" like criteria.name
        }
      }

      if (!criteria.nomisId.isNullOrBlank()) {
        "c.nomis_id" like criteria.nomisId
      }

      if (includeDevices && !criteria.deviceId.isNullOrBlank()) {
        likeCast("da.device_id", criteria.deviceId)
      }
    }
  }
}