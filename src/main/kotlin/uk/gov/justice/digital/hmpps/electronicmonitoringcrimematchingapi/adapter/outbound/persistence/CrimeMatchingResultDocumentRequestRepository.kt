package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CrimeMatchingResultDocumentRequestRepository : JpaRepository<CrimeMatchingResultDocumentRequestEntity, UUID>
