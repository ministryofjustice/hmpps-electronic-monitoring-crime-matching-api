package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.AthenaRole

@Service
class AthenaRoleService(
  @Value("\${services.athena-roles.general:uninitialised}") private val generalRole: String,
) {
  fun fromString(name: String): AthenaRole = enumValues<AthenaRole>().find { it.name == name } ?: AthenaRole.NONE

  fun getRoleFromAuthentication(authentication: Authentication): AthenaRole {
    val roleStrings: List<String> = (
      authentication.authorities
        .map { authority -> authority.authority }
        as MutableList<String>
      )

    val mappedRoles: List<AthenaRole> = mapToOrderedUniqueRoles(roleStrings)

    return mappedRoles.firstOrNull() ?: AthenaRole.NONE
  }

  fun mapToOrderedUniqueRoles(roleStrings: List<String>): List<AthenaRole> = roleStrings
    .map { roleString ->
      enumValues<AthenaRole>()
        .find { it.name == roleString } ?: AthenaRole.NONE
    }.toSet().sortedByDescending { it.priority }

  fun getIamRole(athenaRole: AthenaRole): String = when (athenaRole.name) {
    "ROLE_EM_CRIME_MATCHING_GENERAL_RO" -> generalRole
    else -> ""
  }
}
