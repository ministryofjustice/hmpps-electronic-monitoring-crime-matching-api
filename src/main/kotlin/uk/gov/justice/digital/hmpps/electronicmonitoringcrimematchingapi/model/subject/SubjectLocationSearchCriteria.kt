package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.joda.time.DateTime

data class SubjectLocationSearchCriteria(
  val fromDateTime: DateTime,
  val toDateTime: DateTime,
  val subjectIds: List<String>
)