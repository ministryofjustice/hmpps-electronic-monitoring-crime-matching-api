package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify

import org.springframework.boot.context.properties.ConfigurationProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce

@ConfigurationProperties(prefix = "notify")
data class NotifyProperties(
  val enabled: Boolean,

  val successfulIngestionTemplateId: String,

  val hubEmail: String,

  val policeEmails: Map<PoliceForce, String>
)