package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddHubManagerOccupationMigrationTest {

  private val postgres = PostgreSQLContainer<Nothing>("postgres:17")

  private lateinit var dataSource: DataSource
  private lateinit var jdbcTemplate: JdbcTemplate

  private val hubManagerId = UUID.randomUUID()

  @BeforeAll
  fun setup() {
    postgres.start()

    dataSource = DriverManagerDataSource().apply {
      setDriverClassName("org.postgresql.Driver")
      url = postgres.jdbcUrl
      username = postgres.username
      password = postgres.password
    }

    jdbcTemplate = JdbcTemplate(dataSource)

    // Apply migrations before the occupation column was introduced.
    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .target("16")
      .load()
      .migrate()

    // Insert a hub manager using the original schema.
    jdbcTemplate.update(
      """
      INSERT INTO hub_manager (id, name)
      VALUES (?, ?)
      """.trimIndent(),
      hubManagerId,
      "Test Manager",
    )

    // Apply the occupation migration.
    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .target("17")
      .load()
      .migrate()
  }

  @Test
  fun `it should populate the occupation for existing hub managers`() {
    val occupation = jdbcTemplate.queryForObject(
      "SELECT occupation FROM hub_manager WHERE id = ?",
      String::class.java,
      hubManagerId,
    )

    assertThat(occupation).isEqualTo("MoJ EM Hub Manager")
  }

  @Test
  fun `it should make the occupation column non nullable`() {
    val isNullable = jdbcTemplate.queryForObject(
      """
      SELECT is_nullable
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = 'hub_manager'
        AND column_name = 'occupation'
      """.trimIndent(),
      String::class.java,
    )

    assertThat(isNullable).isEqualTo("NO")
  }

  @Test
  fun `it should make the occupation column have no permanent default`() {
    val defaultValue = jdbcTemplate.queryForObject(
      """
      SELECT column_default
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = 'hub_manager'
        AND column_name = 'occupation'
      """.trimIndent(),
      String::class.java,
    )

    assertThat(defaultValue).isNull()
  }
}
