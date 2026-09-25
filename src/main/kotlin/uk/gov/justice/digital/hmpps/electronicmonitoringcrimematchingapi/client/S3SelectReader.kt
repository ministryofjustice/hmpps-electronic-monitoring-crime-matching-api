package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

interface S3SelectReader {
  fun selectObjectContent(bucket: String, key: String, sqlExpression: String): String
}
