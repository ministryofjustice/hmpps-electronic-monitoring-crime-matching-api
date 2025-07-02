package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectsQueryCacheRepository
import java.time.ZonedDateTime

@Component
class QueryCacheCleanup(
    private val subjectsQueryCacheRepository: SubjectsQueryCacheRepository,
    @Value("\${query-cache.subjects.ttl-hours}") private val cutOffHours: Long,
) {
  companion object {
    private val log = LoggerFactory.getLogger(javaClass)
  }

  @Scheduled(cron = "\${query-cache.subjects.schedule}")
  @Transactional
  fun cleanupStaleQueries() {
    val cutoffTime = ZonedDateTime.now().minusHours(cutOffHours)

    val deleted = subjectsQueryCacheRepository.deleteAllByCreatedAtBefore(cutoffTime)
    log.info("Deleted $deleted stale subjects query records older than $cutoffTime")
  }
}