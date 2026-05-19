package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries.AthenaQueries

@ActiveProfiles("test")
class GetPersonsTest {
  @Test
  fun `it should build a valid query with a name filter`() {
    val personsQueryCriteria = PersonsQueryCriteria(name = "foo")
    val query = GetPersonsQueryBuilder(
      personsQueryCriteria,
    ).build()

    assertThat(query.queryString).isEqualTo(
      AthenaQueries.SelectPersonsByNameLike,
    )
    assertThat(query.parameters).isEqualTo(listOf("'%foo%'", "'%foo%'"))
  }

  @Test
  fun `it should build a valid query with a nomis id filter`() {
    val personsQueryCriteria = PersonsQueryCriteria(nomisId = "foo")
    val query = GetPersonsQueryBuilder(
      personsQueryCriteria,
    ).build()

    assertThat(query.queryString).isEqualTo(
      AthenaQueries.SelectPersonsByNomisIdLike,
    )
    assertThat(query.parameters).isEqualTo(listOf("'%foo%'"))
  }

  @Test
  fun `it should build a valid query with a device id filter`() {
    val personsQueryCriteria = PersonsQueryCriteria(deviceId = 1)
    val query = GetPersonsQueryBuilder(
      personsQueryCriteria,
    ).build()

    assertThat(query.queryString).isEqualTo(
      AthenaQueries.SelectPersonsByDeviceIdLike,
    )
    assertThat(query.parameters).isEqualTo(listOf("1"))
  }
}
