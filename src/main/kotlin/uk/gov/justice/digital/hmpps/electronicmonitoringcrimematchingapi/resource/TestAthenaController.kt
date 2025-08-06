package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.AthenaService

@RestController
class TestAthenaController(
  @Autowired val athenaService: AthenaService,
) {

  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/test-athena",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun test(
    @Parameter(description = "The search criteria for the query", required = true)
    subjectsQueryCriteria: SubjectsQueryCriteria,
  ) : List<Subject> {
    return athenaService.query(subjectsQueryCriteria)
  }
}
