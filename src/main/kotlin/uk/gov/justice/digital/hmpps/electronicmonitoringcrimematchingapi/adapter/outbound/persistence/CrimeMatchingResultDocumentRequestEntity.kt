package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "crime_matching_result_document_request")
class CrimeMatchingResultDocumentRequestEntity(
  @Id
  @Column(name = "document_request_id")
  val documentRequestId: UUID,

  @Column(name = "crime_matching_result_device_wearer_id", nullable = false)
  val crimeMatchingResultDeviceWearerId: UUID,
)
