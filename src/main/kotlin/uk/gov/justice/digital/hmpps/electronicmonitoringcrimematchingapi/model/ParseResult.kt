package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest

class ParseResult(
    val recordCount: Int,
    val records: List<CrimeRecordRequest>,
    val errors: List<String>,
)
