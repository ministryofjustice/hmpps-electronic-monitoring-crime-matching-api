package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.formatter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullIfSentinelDate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import java.time.LocalDateTime

class DeviceActivationResultSetExtractor : AthenaResultSetExtractor<DeviceActivation> {
  override fun extractData(resultSet: ResultSet): List<DeviceActivation> = resultSet
    .rows()
    .drop(1)
    .map { row ->
      val fields = row.data().map { it.varCharValue() }

      DeviceActivation(
        deviceActivationId = fields[0].toLong(),
        deviceId = fields[1].toLong(),
        deviceSerialNumber = fields[2].toLong(),
        uniqueDeviceWearerId = fields[3],
        deviceActivationDate = LocalDateTime.parse(fields[4], formatter),
        deviceDeactivationDate = nullIfSentinelDate(nullableLocalDateTime(fields[5])),
      )
    }
}
