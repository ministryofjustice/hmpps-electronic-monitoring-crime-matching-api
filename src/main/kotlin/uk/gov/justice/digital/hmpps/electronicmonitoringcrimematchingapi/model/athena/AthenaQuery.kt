package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

abstract class AthenaQuery(
  open val queryString: String,
  open val parameters: Array<String>,
)

class AthenaSubjectSearchQuery(
  override val queryString: String,
  override val parameters: Array<String>,
) : AthenaQuery(queryString, parameters)

class AthenaSubjectQuery(
  override val queryString: String,
  override val parameters: Array<String>,
) : AthenaQuery(queryString, parameters)