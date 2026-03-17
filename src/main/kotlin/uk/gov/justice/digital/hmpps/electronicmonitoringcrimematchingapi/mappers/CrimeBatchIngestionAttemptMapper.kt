package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.BreakdownByCrimeTypeResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptResponse
<<<<<<< Updated upstream
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttemptSummary

@Component
class CrimeBatchIngestionAttemptMapper(
  val ingestionAttemptCrimesByTypeMapper: IngestionAttemptCrimesByTypeMapper,
  val crimeBatchEmailAttachmentErrorMapper: CrimeBatchEmailAttachmentErrorMapper,
) {
  fun toDto(
    summary: CrimeBatchIngestionAttemptSummary,
  ): CrimeBatchIngestionAttemptResponse = CrimeBatchIngestionAttemptResponse(
    ingestionAttemptId = summary.ingestionAttempt.ingestionAttemptId,
    ingestionStatus = summary.ingestionAttempt.ingestionStatus.name,
    policeForceArea = summary.ingestionAttempt.policeForceArea ?: "",
    batchId = summary.ingestionAttempt.batchId ?: "",
    matches = summary.ingestionAttempt.matches,
    createdAt = summary.ingestionAttempt.createdAt.toString(),
    fileName = summary.ingestionAttempt.fileName,
    submitted = summary.ingestionAttempt.submitted ?: 0,
    successful = summary.ingestionAttempt.successful ?: 0,
    failed = summary.ingestionAttempt.failed ?: 0,
    crimesByCrimeType = summary.crimesByCrimeType.map(ingestionAttemptCrimesByTypeMapper::toDto),
    validationErrors = summary.validationErrors.map(crimeBatchEmailAttachmentErrorMapper::toDto),
  )
=======
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.ValidationErrorResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttemptSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType

@Component
class CrimeBatchIngestionAttemptMapper {
  fun toDto(summary: CrimeBatchIngestionAttemptSummary): CrimeBatchIngestionAttemptResponse {
    val attempt = summary.ingestionAttempt

    return CrimeBatchIngestionAttemptResponse(
      ingestionAttemptId = attempt.getIngestionAttemptId().toString(),
      ingestionStatus = attempt.getIngestionStatus(),
      policeForceArea = attempt.getPoliceForceArea(),
      crimeBatchId = attempt.getCrimeBatchId()?.toString(),
      batchId = attempt.getBatchId(),
      matches = attempt.getMatches(),
      createdAt = attempt.getCreatedAt().toString(),
      fileName = attempt.getFileName(),
      isCrimeBatch = attempt.getIsCrimeBatch(),
      failureSubCategory = attempt.getFailureSubCategory(),
      submittedCount = attempt.getSubmitted()?.toLong(),
      ingestedCount = attempt.getSuccessful()?.toLong(),
      failedCount = attempt.getFailed()?.toLong(),
      breakdownByCrimeType = summary.crimesByCrimeType.map { row ->
        BreakdownByCrimeTypeResponse(
          crimeType = crimeTypeName(row.getCrimeType()),
          submitted = row.getSubmitted(),
          ingested = row.getSuccessful(),
          failedValidation = row.getFailed(),
        )
      },
      validationErrors = summary.validationErrors.map { error ->
        ValidationErrorResponse(
          crimeReference = error.getCrimeReference() ?: "",
          errorType = error.getErrorType(),
          requiredAction = requiredActionFor(error.getErrorType()),
        )
      },
    )
  }

  private fun crimeTypeName(crimeType: String?): String {
    if (crimeType == null) return "Unknown"
    return try {
      CrimeType.valueOf(crimeType).value ?: "Unknown"
    } catch (e: Exception) {
      "Unknown"
    }
  }

  private fun requiredActionFor(errorType: String): String {
    val actions = mapOf(
      "INVALID_COLUMN_COUNT" to "Check the submitted CSV file and ensure all required columns are present.",
      "MISSING_BATCH_ID" to "Provide a valid batch ID in the format <ForceCode><YYYYMMDD>.",
      "INVALID_BATCH_ID_FORMAT" to "Ensure the valid batch ID starts with the correct force code followed by 8 digits (YYYYMMDD).",
      "INVALID_BATCH_ID_DATE" to "Check the date portion of the batch ID is a valid calendar date in YYYYMMDD format.",
      "MISSING_CRIME_REFERENCE" to "Provide a crime reference for this row.",
      "INVALID_TEXT" to "Check this field value and remove any unsupported characters.",
      "INVALID_NUMBER" to "Ensure this field contains a valid number.",
      "INVALID_DATE_FORMAT" to "Dated must be in yyyyMMddHHmmss format (e.g. 20260128103000).",
      "CRIME_DATE_TIME_to_AFTER_FROM" to "Ensure the end date/time is after the start date/time",
      "CRIME_DATE_TIME_EXCEEDS_WINDOW" to "The time between start and end must be 12 hours or less.",
      "INVALID_ENUM" to "Check the allowed values for this field and correct the entry.",
      "DEPENDENT_LOCATION_DATA" to "Provide entries for both easting and northing or both latitude and longitude.",
      "MULTIPLE_LOCATION_DATA_TYPES" to "Provide entries for either easting/northing or latitude/longitude ... not both.",
      "INVALID_LOCATION_DATA_RANGE" to "Check that the coordinate values are within the expected boundries of Great Britian.",
      "MISSING_LOCATION_DATA" to "Provide entries for either easting/northing or latitude/longitude coordinates.",
    )
    return actions[errorType] ?: errorType
  }
>>>>>>> Stashed changes
}
