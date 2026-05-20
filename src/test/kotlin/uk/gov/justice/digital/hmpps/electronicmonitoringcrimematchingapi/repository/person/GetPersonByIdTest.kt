package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries.AthenaQueries

@ActiveProfiles("test")
class GetPersonByIdTest {
  @Test
  fun `it should build a valid query`() {
    val id = "foo"
    val query = GetPersonByIdQueryBuilder(id).build()

    assertThat(query.queryString).isEqualTo(AthenaQueries.SelectPersonById)
    assertThat(query.parameters).isEqualTo(listOf("'foo'"))
  }
}
