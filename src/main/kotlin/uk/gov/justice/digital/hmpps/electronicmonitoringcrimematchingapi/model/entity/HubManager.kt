package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "hub_manager")
data class HubManager(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Column(nullable = false)
  val name: String,

  @Column(name = "signature_image", columnDefinition = "bytea")
  var signatureImage: ByteArray? = null,

  @Column(name = "signature_image_content_type")
  var signatureImageContentType: String? = null,
)
