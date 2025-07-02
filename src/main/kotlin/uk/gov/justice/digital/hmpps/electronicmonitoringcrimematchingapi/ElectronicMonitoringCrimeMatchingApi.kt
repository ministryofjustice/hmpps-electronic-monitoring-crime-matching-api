package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ElectronicMonitoringCrimeMatchingApi

fun main(args: Array<String>) {
  runApplication<ElectronicMonitoringCrimeMatchingApi>(*args)
}
