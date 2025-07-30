package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
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
  fun test() {
    athenaService.query()
  }
}
