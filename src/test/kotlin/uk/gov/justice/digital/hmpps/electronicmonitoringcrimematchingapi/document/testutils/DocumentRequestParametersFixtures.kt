package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.AuthorisingManagerDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.CrimeDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.DeviceWearerDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ReportPeriod
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ReportRequestDetails
import java.time.Instant
import java.time.LocalDate

object DocumentRequestParametersFixtures {
  fun givenContextualReportRequestParameters(
    reportPeriod: ReportPeriod = ReportPeriod(
      start = Instant.parse("2026-10-01T01:02:03Z"),
      end = Instant.parse("2026-10-02T04:05:06Z"),
    ),
  ) = ContextualReportRequestParameters(
    additionalInformation = "",
    authorisingManager = AuthorisingManagerDetails(
      address = "123 The Street, London, UK, AB12 3CD",
      name = "John Smith",
      occupation = "AC Hub Manager",
    ),
    crime = CrimeDetails(
      batchId = "AVS20260101",
      reference = "123456789",
      type = "Robbery",
    ),
    deviceWearer = DeviceWearerDetails(
      address = "456 The Avenue, London, UK, AB12 3CD",
      dateOfBirth = LocalDate.of(1990, 1, 1),
      emsId = "DEVWR123456",
      pncNumber = "PNC123456",
      name = "Jane Doe",
      offenderManager = "Probation Practitioner",
    ),
    positionSegments = listOf(
      listOf(
        Position(
          direction = 90,
          gpsDate = Instant.parse("2026-10-01T00:00:00Z"),
          latitude = 51.5074,
          longitude = -0.1278,
          precision = 5,
          speed = 10,
          sequenceNumber = 1,
        ),
      ),
    ),
    reportPeriod = reportPeriod,
    reportRequest = ReportRequestDetails(
      requestedAt = Instant.parse("2026-10-03T11:12:13Z"),
      requestedBy = "Police Officer",
      policeForceArea = "Metropolitan",
    ),
  )

  fun givenRawContextualReportRequestParameters() = mapOf(
    "additionalInformation" to "",
    "authorisingManager" to mapOf(
      "address" to "123 The Street, London, UK, AB12 3CD",
      "name" to "John Smith",
      "occupation" to "AC Hub Manager",
    ),
    "crime" to mapOf(
      "batchId" to "AVS20260101",
      "reference" to "123456789",
      "type" to "Robbery",
    ),
    "deviceWearer" to mapOf(
      "address" to "456 The Avenue, London, UK, AB12 3CD",
      "dateOfBirth" to "1990-01-01",
      "emsId" to "DEVWR123456",
      "pncNumber" to "PNC123456",
      "name" to "Jane Doe",
      "offenderManager" to "Probation Practitioner",
    ),
    "positionSegments" to listOf(
      listOf(
        mapOf(
          "direction" to 90,
          "gpsDate" to "2026-10-01T00:00:00Z",
          "latitude" to 51.5074,
          "longitude" to -0.1278,
          "precision" to 5,
          "speed" to 10,
          "sequenceNumber" to 1,
        ),
      ),
    ),
    "reportPeriod" to mapOf(
      "start" to "2026-10-01T00:00:00Z",
      "end" to "2026-10-02T00:00:00Z",
    ),
    "reportRequest" to mapOf(
      "requestedAt" to "2026-10-03T00:00:00Z",
      "requestedBy" to "Police Officer",
      "policeForceArea" to "Metropolitan",
    ),
  )
}
