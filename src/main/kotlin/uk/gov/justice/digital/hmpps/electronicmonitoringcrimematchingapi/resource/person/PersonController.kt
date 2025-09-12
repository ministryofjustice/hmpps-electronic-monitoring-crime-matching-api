package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.person

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person.PersonService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/persons", produces = ["application/json"])
class PersonController(
  @Autowired val personService: PersonService,
) {

  @Operation(
    tags = ["Person"],
    summary = "Search for persons",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getPersons(
    authentication: Authentication,
    @Parameter(description = "The search criteria for the query", required = true)
    personsQueryCriteria: PersonsQueryCriteria,
  ): ResponseEntity<List<PersonDto>> {
    if (!personsQueryCriteria.isValid()) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Query parameters are invalid: $personsQueryCriteria",
      )
    }
    val result = personService.getPersons(personsQueryCriteria, authentication.name)
    return ResponseEntity.ok(result)
  }

  @Operation(
    tags = ["Person"],
    summary = "Get a persons",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{personId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getPerson(
    authentication: Authentication,
    @PathVariable personId: Long,
  ): ResponseEntity<PersonDto> {
    val person = personService.getPerson(personId, authentication.name)

    return ResponseEntity.ok(person)
  }
}
