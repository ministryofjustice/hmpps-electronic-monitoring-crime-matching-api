package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.person

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person.PersonService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CASELOAD__RO')")
@RequestMapping("/persons", produces = ["application/json"])
class PersonController(
  val personService: PersonService,
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
    @Parameter(description = "The search criteria for the query", required = true)
    personsQueryCriteria: PersonsQueryCriteria,
    @Parameter(
      description = "Page number (0-based). Defaults to 0.",
      example = "0",
    )
    @RequestParam(defaultValue = "0")
    page: Int = 0,
    @Parameter(
      description = "Number of items per page. Defaults to 30.",
      example = "30",
    )
    @RequestParam(defaultValue = "30")
    pageSize: Int = 30,
  ): ResponseEntity<PagedResponse<PersonResponse>> {
    if (!personsQueryCriteria.isValid()) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Query parameters are invalid: $personsQueryCriteria",
      )
    }
    val result = personService.getPersons(personsQueryCriteria, page, pageSize)
    return ResponseEntity.ok(
      PagedResponse(
        data = result.data.map { PersonResponse(it) },
        pageSize = pageSize,
        pageNumber = page,
        pageCount = result.pageCount,
      ),
    )
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
    @PathVariable personId: String,
  ): ResponseEntity<Response<PersonResponse>> {
    val person = personService.getPerson(personId)

    return ResponseEntity.ok(
      Response(
        PersonResponse(person),
      ),
    )
  }
}
