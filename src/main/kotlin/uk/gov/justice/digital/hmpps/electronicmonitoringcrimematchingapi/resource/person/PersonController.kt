package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.person

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person.PersonService

@RestController
//@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
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
        "Query must have at least one parameter specified: $personsQueryCriteria",
      )
    }
    val result = personService.getPersonsQueryResults(personsQueryCriteria, authentication.name)
    return ResponseEntity.ok(result)
  }
}
