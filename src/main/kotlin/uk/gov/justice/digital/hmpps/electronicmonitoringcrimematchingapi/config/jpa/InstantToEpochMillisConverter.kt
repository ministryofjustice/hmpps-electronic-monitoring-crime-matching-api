package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.jpa

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.Instant

@Converter
class InstantToEpochMillisConverter : AttributeConverter<Instant, Long> {
  override fun convertToDatabaseColumn(attribute: Instant?): Long? = attribute?.toEpochMilli()

  override fun convertToEntityAttribute(dbData: Long?): Instant? = dbData?.let { Instant.ofEpochMilli(it) }
}
