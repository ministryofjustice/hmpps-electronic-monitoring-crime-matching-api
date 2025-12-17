package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto

class ParseResult(
  val recordCount: Int,
  val records: List<CrimeRecordDto>,
  val errors: List<String>,
)
