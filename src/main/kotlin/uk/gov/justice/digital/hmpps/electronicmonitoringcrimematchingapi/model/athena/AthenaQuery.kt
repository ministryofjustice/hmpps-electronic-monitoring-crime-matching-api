package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import java.io.Serializable

class AthenaQuery(
  val queryString: String,
  val parameters: Array<String>,
) : Serializable {
  override fun toString(): String = this.queryString
}
