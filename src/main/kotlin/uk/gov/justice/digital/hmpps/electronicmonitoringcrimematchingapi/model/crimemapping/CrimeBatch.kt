package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping

import java.util.Date

data class CrimeBatch(
  val policeForce: String,
  val time: String,
  val matches: Int,
  val ingestionDate: Date,
  val caseloadDate: Date,
  val batchId: String,
  val startDate: Date,
  val endDate: Date,
  val algorithmVersion: String,
  val filename: String,
  val size: String,
  val status: String
)