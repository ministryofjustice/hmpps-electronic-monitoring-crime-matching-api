package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import com.amazon.athena.jdbc.AthenaDriver
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class AthenaService() {

  val connectionDetails = Properties()

  init {
    connectionDetails.setProperty("Workgroup", "396913731313-default")
    connectionDetails.setProperty("Region", "eu-west-2")
    connectionDetails.setProperty("Catalog", "AwsDataCatalog")
//    connectionDetails.setProperty("Database","crime_matching_test_db")
    connectionDetails.setProperty("OutputLocation", "s3://emds-dev-athena-query-results-20240917144028307600000004")
    connectionDetails.setProperty("CredentialsProvider", "DefaultChain")
  }

  fun query() {
    val query = "SELECT * FROM crime_matching_test_db.person LIMIT 10"

    val url = "jdbc:athena://"
    val driver = AthenaDriver()
    val connection = driver.connect(url, connectionDetails)
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(query)
  }
}
