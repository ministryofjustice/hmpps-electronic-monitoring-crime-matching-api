package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

object CrimeBatchIngestionAttemptBuilder {
  fun aCrimeBatchIngestionAttempt(): CrimeBatchIngestionAttempt {
    val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )
    return crimeBatchIngestionAttempt
  }
}

object CrimeBatchEmailBuilder {
  fun aCrimeBatchEmail(
    crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt? = null
  ): CrimeBatchEmail {
    val attempt = crimeBatchIngestionAttempt?: CrimeBatchIngestionAttemptBuilder.aCrimeBatchIngestionAttempt()
    val crimeBatchEmail = CrimeBatchEmail(
      crimeBatchIngestionAttempt = attempt,
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
    )
    attempt.crimeBatchEmail = crimeBatchEmail
    return crimeBatchEmail
  }
}

object CrimeBatchEmailAttachmentBuilder {
  fun aCrimeBatchEmailAttachment(
    crimeBatchEmail: CrimeBatchEmail? = null
  ): CrimeBatchEmailAttachment {
    val email = crimeBatchEmail ?: CrimeBatchEmailBuilder.aCrimeBatchEmail()
    val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
      crimeBatchEmail = email,
      fileName = "filename",
      rowCount = 1,
    )
    email.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)
    return crimeBatchEmailAttachment
  }
}

object CrimeBatchBuilder {
  fun aCrimeBatch(
    id: UUID = UUID.randomUUID(),
    crimeBatchEmailAttachment: CrimeBatchEmailAttachment? = null,
  ): CrimeBatch {
    val crimeBatch = CrimeBatch(
      id = id,
      batchId = "batchId",
      crimeBatchEmailAttachment = crimeBatchEmailAttachment?: CrimeBatchEmailAttachmentBuilder.aCrimeBatchEmailAttachment(),
    )
    return crimeBatch
  }
}

object CrimeBuilder {
  fun aCrime(): Crime {
    val crime = Crime(
      policeForceArea = PoliceForce.METROPOLITAN,
      crimeReference = "CRI00000001",
    )
    return crime
  }
}

object CrimeVersionBuilder {
  fun aCrimeVersion(
    id: UUID = UUID.randomUUID(),
    crime: Crime = CrimeBuilder.aCrime(),
    crimeTypeId: CrimeType = CrimeType.AB,
    crimeDateTimeFrom: LocalDateTime = LocalDateTime.of(2025, 1, 25, 8, 30),
    crimeDateTimeTo: LocalDateTime = LocalDateTime.of(2025, 1, 25, 8, 30),
    easting: Double? = null,
    northing: Double? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    crimeText: String = "",
  ): CrimeVersion {
    val crimeVersion = CrimeVersion(
      id = id,
      crime = crime,
      crimeTypeId = crimeTypeId,
      crimeDateTimeFrom = crimeDateTimeFrom,
      crimeDateTimeTo = crimeDateTimeTo,
      easting = easting,
      northing = northing,
      latitude = latitude,
      longitude = longitude,
      crimeText = crimeText,
    )
    return crimeVersion
  }
}

object CrimeMatchingRunBuilder {
  fun aCrimeMatchingRun(
    crimeBatch: CrimeBatch? = null
  ): CrimeMatchingRun {
    val crimeMatchingRun = CrimeMatchingRun(
      crimeBatch = crimeBatch ?: CrimeBatchBuilder.aCrimeBatch(),
      algorithmVersion = "algoVers",
      triggerType = CrimeMatchingTriggerType.AUTO,
      status = CrimeMatchingStatus.SUCCESS,
      matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
      matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 31, 0),
      results = mutableListOf()
    )

    return crimeMatchingRun
  }
}

object CrimeMatchingResultBuilder {
  fun aCrimeMatchingResult(
    crimeVersion: CrimeVersion? = null,
    crimeMatchingRun: CrimeMatchingRun? = null
  ): CrimeMatchingResult {
    val crimeMatchingResult = CrimeMatchingResult(
      crimeVersion = crimeVersion ?: CrimeVersionBuilder.aCrimeVersion(),
      crimeMatchingRun = crimeMatchingRun ?: CrimeMatchingRunBuilder.aCrimeMatchingRun(),
      deviceWearers = mutableListOf(),
    )
    return crimeMatchingResult
  }
}

object CrimeMatchingResultDeviceWearerBuilder {
  fun aCrimeMatchingResultDeviceWearer(
    crimeMatchingResult: CrimeMatchingResult? = null
  ): CrimeMatchingResultDeviceWearer {
    val deviceWearer = CrimeMatchingResultDeviceWearer(
      crimeMatchingResult = crimeMatchingResult ?: CrimeMatchingResultBuilder.aCrimeMatchingResult(),
      deviceId = 1L,
      name = "name",
      nomisId = "nomisId",
      positions = mutableListOf()
    )
    return deviceWearer
  }
}

object CrimeMatchingResultPositionBuilder {
  fun aCrimeMatchingResultPosition(
    deviceWearer: CrimeMatchingResultDeviceWearer? = null
  ): CrimeMatchingResultPosition {
    val crimeMatchingResultPosition = CrimeMatchingResultPosition(
      crimeMatchingResultDeviceWearer = deviceWearer ?: CrimeMatchingResultDeviceWearerBuilder.aCrimeMatchingResultDeviceWearer(),
      latitude = 1.0,
      longitude = 1.0,
      capturedDateTime = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
      sequenceLabel = "seqLabel",
      confidenceCircle = 10
    )
    return crimeMatchingResultPosition
  }
}
