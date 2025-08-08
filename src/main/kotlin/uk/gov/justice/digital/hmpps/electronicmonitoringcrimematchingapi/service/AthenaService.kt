package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectRowMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import java.sql.Driver

@Service
class AthenaService() {
  val outputLocation = ""
  val workGroup = ""
  // Swap for sts assume role in other envs
  val credentialsProvider = "DefaultChain"

  fun query(subjectsQueryCriteria : SubjectsQueryCriteria): List<Subject> {
    val url = "jdbc:awsathena://Region=eu-west-2;" +
      "OutputLocation=${outputLocation};" +
      "WorkGroup=${workGroup};" +
      "CredentialsProvider=${credentialsProvider};"

    val driver = Class.forName("com.amazon.athena.jdbc.AthenaDriver").getDeclaredConstructor().newInstance() as Driver
    val dataSource = SimpleDriverDataSource(driver, url)

//    val jdbcTemplate = JdbcTemplate(dataSource)

    val sql = "SELECT * FROM crime_matching_test_db.person LIMIT 10 WHERE person_name LIKE :personName OR nomis_id LIKE :nomisId"
    val params = mapOf("personName" to "%${subjectsQueryCriteria.name}%", "nomisId" to "%${subjectsQueryCriteria.nomisId}%")

    val namedTemplate = NamedParameterJdbcTemplate(dataSource)

    return namedTemplate.query(sql, params, SubjectRowMapper())
  }
}
